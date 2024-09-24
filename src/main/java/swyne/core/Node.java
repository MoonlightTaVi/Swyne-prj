package swyne.core;

import main.Main;
import swyne.BondCollector;
import swyne.Sentence;
import swyne.Word;

import java.util.*;

public class Node {
    protected Node extendsNode;
    protected final String name;
    protected final Word actor;
    protected List<Line> code = new ArrayList<>();
    protected final Map<String, String> args = new HashMap<>();

    public Node(String name) {
        this(name, new Word(name, 0));
    }

    public Node(String name, Word actor) {
        this(name, actor, null);
    }

    public Node(String name, Word actor, Node extendsNode) {
        this.name = name.toLowerCase();
        this.extendsNode = extendsNode;
        this.actor = actor;
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
                if (!matchActor(actor)) {
                    continue;
                }
                Word nextVerb = verb;
                while (nextVerb != null) {
                    //System.out.println(nextVerb);
                    //System.out.println(nextVerb.getLemmas());
                    if (Main.compareLists(nextVerb.getLemmas(), List.of("есть")) > 0) {
                        if (Core.nodes.containsKey(name)) {
                            nextVerb = nextVerb.getNextVerb();
                            continue;
                        }
                        //System.out.println(111);
                        Optional<Word> extendsWord = nextVerb.getBondsWithCondition("АРГУМЕНТ", w -> w.getCasesJoined().contains("им")).stream().findFirst();
                        final List<String>[] extendsNames = new List[]{new ArrayList<>()};
                        extendsWord.ifPresent(ext -> extendsNames[0] = ext.getLemmas());
                        //System.out.println(extendsWord);
                        for (String extendsName : extendsNames[0]) {
                            if (Core.nodes.containsKey(extendsName.toLowerCase())) {
                                if (Core.nodes.get(extendsName.toLowerCase()) instanceof Value) {
                                    Value newValue = new Value(name, actor);
                                    newValue.setCode(code);
                                    Core.nodes.put(name.toLowerCase(), newValue);
                                    newValue.compile();
                                    return false;
                                } else {
                                    this.extendsNode = Core.nodes.get(extendsName.toLowerCase());
                                    Core.nodes.put(name.toLowerCase(), this);
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
                for (Word action : actor.getBonds("ДЕЙСТВИЕ")) {
                    if (action != verb) {
                        continue;
                    }
                    if (line.getCondition() != null) {
                        for (Word conditionActor : line.getCondition().getActors()) {
                            if (Main.compareLists(actor.getLemmas(), conditionActor.getLemmas()) == 0) {
                                continue;
                            }
                            Set<Word> ignore = new HashSet<>(new BondCollector(actor).collect("ДЕЙСТВИЕ").get());
                            ignore.remove(verb);
                            if (!actor.compareLemmas(conditionActor, ignore)) {
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
    }

    public void compile() {
        for (Line line : code) {
            if (line.getCondition() == null) {
                boolean cont = execute(line);
                if (!cont) {
                    return;
                }
            }
        }
    }

    public int check(Sentence condition) {
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

    public boolean matchActor(Word matchTo) {
        if (this.actor == null) {
            System.err.printf("\"%s\" does not have an actor property.%n", this.name);
            return false;
        }
        return actor.matchActor(matchTo);
    }

    @Override
    public String toString() {
        return String.format("\"%s\": %s", name, args.toString());
    }
}
