package swyne;

import main.Main;
import swyne.AnalyzeTextUtil.*;
import java.util.*;

public class Core {
    public static Map<String, Node> nodes = new HashMap<>();
    public static void setup() {
        nodes.put("величина", new Value("величина"));
    }
    public static class Node {
        protected Node extendsNode;
        protected final String name;
        protected List<Line> code = new ArrayList<>();
        protected final Map<String, String> args = new HashMap<>();
        public Node(String name) {
            this.name = name.toLowerCase();
        }
        public Node(String name, Node extendsNode) {
            this.name = name.toLowerCase();
            this.extendsNode = extendsNode;
        }
        public void setCode(List<Line> code) {
            this.code = code;
        }
        public void addLine(Line line) {
            code.add(line);
        }
        public void addArg(String argName, String arg) {
            args.put(argName, arg);
        }
        public List<Line> getCode() {
            return code;
        }
        public boolean execute(Line line) {
            Sentence sentence = line.getLine();
            List<Word> verbs = sentence.getVerbs();
            for (Word verb : verbs) {
                for (Word actor : verb.getBonds("АКТЁР")) {
                    if (!actor.getLemmas().contains(this.getName())) {
                        continue;
                    }
                    Word nextVerb = verb;
                    while (nextVerb != null) {
                        //System.out.println(nextVerb);
                        //System.out.println(nextVerb.getLemmas());
                        if (Main.compareLists(nextVerb.getLemmas(), List.of("есть")) > 0) {
                            if (nodes.containsKey(name)) {
                                nextVerb = nextVerb.getNextVerb();
                                continue;
                            }
                            //System.out.println(111);
                            Optional<Word> extendsWord = nextVerb.getBondsWithCondition("АРГУМЕНТ", w -> w.getCasesJoined().contains("им")).stream().findFirst();
                            final List<String>[] extendsNames = new List[]{new ArrayList<>()};
                            extendsWord.ifPresent(ext -> extendsNames[0] = ext.getLemmas());
                            //System.out.println(extendsWord);
                            for (String extendsName : extendsNames[0]) {
                                if (nodes.containsKey(extendsName.toLowerCase())) {
                                    if (nodes.get(extendsName.toLowerCase()) instanceof Value) {
                                        Value newValue = new Value(name);
                                        newValue.setCode(code);
                                        nodes.put(name.toLowerCase(), newValue);
                                        newValue.compile();
                                        return false;
                                    } else {
                                        this.extendsNode = nodes.get(extendsName.toLowerCase());
                                        nodes.put(name.toLowerCase(), this);
                                    }
                                }
                            }
                        }
                        if (Main.compareLists(nextVerb.getLemmas(), List.of("мочь")) > 0) {
                            findAndExecute(line, nextVerb);
                        }
                        nextVerb = nextVerb.getNextVerb();
                    }
                }
            }
            return true;
        }
        public void findAndExecute(Line start, Word verb) {
            for (Word actor : start.getLine().getActors()) {
                boolean started = false;
                for (Line line : code) {
                    if (line != start && !started) {
                        continue;
                    } else if (line == start) {
                        started = true;
                        continue;
                    }
                    if (!actor.getBonds("ДЕЙСТВИЕ").contains(verb)) {
                        continue;
                    }
                    if (line.getCondition() != null) {
                        for (Word conditionActor : line.getCondition().getActors()) {
                            if (Main.compareLists(actor.getLemmas(), conditionActor.getLemmas()) == 0) {
                                continue;
                            }
                            if (!actor.compareLemmas(conditionActor)) {
                                continue;
                            }
                            if (check(line.getCondition()) == 1) {
                                execute(line);
                                return;
                            }
                        }
                    }
                }
            }
        }
        public void compile() {
            for (Line line : code) {
                if (line.condition == null) {
                    boolean cont = execute(line);
                    if (!cont) {
                        return;
                    }
                }
            }
        }
        public int check(AnalyzeTextUtil.Sentence condition) {
            return -1;
        }
        public String getName() {
            return name;
        }
        public Node getExtends() {
            return extendsNode;
        }

        public void setExtends(Node setTo) {
            this.extendsNode = setTo;
        }

        @Override
        public String toString() {
            return String.format("\"%s\": %s", name, args.toString());
        }
    }
    public static class Line {
        private final Sentence line;
        private Sentence condition = null;
        public Line(Sentence line) {
            this.line = line;
        }
        public Line(Sentence line, Sentence condition) {
            this.line = line;
            this.condition = condition;
        }
        public Sentence getLine() {
            return line;
        }
        public Sentence getCondition() {
            return condition;
        }
    }
    public static class Value extends Node {
        public Value(String name) {
            super(name);
        }

        @Override
        public boolean execute(Line line) {
            super.execute(line);
            Sentence sentence = line.getLine();
            List<Word> verbs = sentence.getVerbs();
            for (Word verb : verbs) {
                for (Word actor : verb.getBonds("АКТЁР")) {
                    if (!actor.getLemmas().contains(this.getName())) {
                        continue;
                    }
                    Word nextVerb = verb;
                    while (nextVerb != null) {
                        if (Main.compareLists(nextVerb.getLemmas(), List.of("находиться", "лежать")) > 0) {
                            BondCollector bondCollector = new BondCollector(nextVerb);
                            bondCollector.goTo("ЧИСЛОВОЙАРГУМЕНТ")
                                    .goTo("ПРЕДЛ").filter(w -> w.getName().equals("от"))
                                    .collect("ОБЪЕКТ").getFirst()
                                    .ifPresent(w -> addArg("минимум", w.getName()));
                            bondCollector.goBackTo("ПРЕДЛ")
                                    .filter(w -> w.getName().equals("до"))
                                    .collect("ОБЪЕКТ").getFirst()
                                    .ifPresent(w -> addArg("максимум", w.getName()));
                        }
                        if (Main.compareLists(nextVerb.getLemmas(), List.of("равняться")) > 0) {
                            nextVerb.getFirstBond("ЧИСЛОВОЙАРГУМЕНТ")
                                    .ifPresent(w -> addArg("значение", w.getName()));
                        }
                        if (Main.compareLists(nextVerb.getLemmas(), List.of("увеличиться", "уменьшиться")) > 0) {
                            Word sumW = new BondCollector(nextVerb).goTo("ЧИСЛОВОЙАРГУМЕНТ")
                                    .goTo("ПРЕДЛ").filter(w -> w.getName().equals("на"))
                                    .collect("ОБЪЕКТ").getFirst().orElse(null);
                            if (sumW != null) {
                                try {
                                    if (!args.containsKey("значение")) {
                                        System.err.printf("No value has been assigned to \"%s\".%n", actor.getName());
                                        continue;
                                    }
                                    double value = Double.parseDouble(args.get("значение"));
                                    double sum = Double.parseDouble(sumW.getName());
                                    if (nextVerb.getLemmas().contains("увеличиться")) {
                                        value += sum;
                                    } else {
                                        value -= sum;
                                    }
                                    args.put("значение", String.valueOf(value));
                                } catch (IllegalArgumentException e) {
                                    if (args.containsKey("значение")) {
                                        System.err.printf("Cannot convert either \"%s\" or \"%s\" to double in \"%s\".%n", sumW.getName(), args.get("значение"), actor.getName());
                                    } else {
                                        System.err.printf("Cannot convert \"%s\" to double in \"%s\".%n", sumW.getName(), actor.getName());
                                    }
                                }
                            }
                        }
                        nextVerb = nextVerb.getNextVerb();
                    }
                }
            }
            return true;
        }
        @Override
        public int check(Sentence condition) {
            if (super.check(condition) == 0) {
                return 0;
            }
            List<Word> actors = condition.getActors();
            for (Word actor : actors) {
                if (!actor.getLemmas().contains(this.getName())) {
                    continue;
                }
                Set<Word> comparisons = actor.getBonds("СРАВН");
                if (comparisons.isEmpty()) {
                    continue;
                }
                for (Word comparison : comparisons) {
                    Optional<Word> number =  comparison.getFirstBond("ЧЕМ");
                    if (number.isPresent()) {
                        double value = 0.0;
                        try {
                            value = Double.parseDouble(args.get("значение"));
                        } catch (IllegalArgumentException e) {
                            System.err.printf("Couldn't parse \"значение\" of \"%s\".%n", name);
                        }
                        double d = 0.0;
                        try {
                            d = Double.parseDouble(number.get().getName());
                        } catch (IllegalArgumentException e) {
                            System.err.printf("Couldn't parse \"%s\" in \"%s\".%n", number.get().getName(), name);
                        }
                        //System.out.println(comparison.getLemmas());
                        if (comparison.getLemmas().contains("мало")) {
                            if (value < d) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                        if (comparison.getLemmas().contains("много")) {
                            if (value > d) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    }
                }
            }
            return 0;
        }
    }

    public static class Location extends Node {
        public Location(String name) {
            super(name);
        }

    }

    public static void addNode(String name, String extendsNodeName) {
        if (!Core.nodes.containsKey(extendsNodeName.toLowerCase())) {
            System.err.printf("swyne.Core does not have a node with name: \"%s\".%n", extendsNodeName);
            return;
        }
        if (Core.nodes.containsKey(name.toLowerCase())) {
            System.err.printf("swyne.Core does already have a node with name: \"%s\".%n", name);
            return;
        }
        Node extendsNode = Core.nodes.get(extendsNodeName.toLowerCase());
        Node newNode = new Node(name.toLowerCase(), extendsNode);
        Core.nodes.put(name.toLowerCase(), newNode);
    }
}
