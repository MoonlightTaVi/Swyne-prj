package swyne.utils;

import java.util.*;
import java.util.stream.Collectors;

import main.Main;
import regex.RegexUtils.*;
import swyne.Sentence;
import swyne.Word;
import swyne.core.Core;
import swyne.core.Node;
import swyne.core.Line;

public class CommandParser {
    private static final List<Node> nodes = new ArrayList<>();
    private static final RegexHelper regex = new RegexHelper();
    private static String code = "";
    public static void setup() {
        regex.add("(?<term>(.*?)) - это (?<definition>(.*?))([.?!])");
        regex.add("Если (?<condition>(.*?)), то (?<command>(.*?))([.?!])");
        code = new FileReader().setDirectory("src/main/resources/code")
                .add("test.txt")
                .readAllToString();
    }
    public static void main(String[] args) {
        //utils.PassedTime pt = new utils.PassedTime("swyne.utils.CommandParser.main()");
        Main.setup();
        parseLine(code);
        compileAll();
        //pt.makeStamp("Initialization finished");
        /*parseLine("Длина - это величина, она лежит в пределах от 0 до 100, равняется 40.");
        parseLine("Она может быть больше 50 или меньше 50, может быть больше 30.");
        parseLine("Если она меньше 50, то она увеличится на 20.");
        parseLine("Если она больше 50, то она уменьшится на 20.");
        parseLine("Если она больше 30, то она уменьшится на 10.");
        //pt.makeStamp("Parsing finished");
        compileAll();*/
        System.out.println("Result:");
        System.out.println(Core.nodes.get("величина"));
        System.out.println(Core.nodes.get("длина"));
        System.out.println(Core.nodes.get("илья"));
    }
    public static void parseLine(String text) {
        String[] sentences = text.split("(?<=[.!?/';])\\s+");
        for (String sentence : sentences) {
            System.out.printf("Parsing: \"%s\"%n", sentence);
            Map<String, String> groups = new HashMap<>();
            regex.match(sentence);
            while (regex.find()) {
                groups.putAll(regex.getMatchResults().stream()
                        .map(Entry::getValue)
                        .flatMap(map -> map.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }

            if (groups.containsKey("term")) {
                sentence = String.format("%s есть %s.", groups.get("term"), groups.get("definition"));
            }
            Sentence parsedSentence;
            Sentence condition = null;
            if (groups.containsKey("condition")) {
                parsedSentence = AnalyzeTextUtil.makeDependencies(groups.get("command"));
                condition = AnalyzeTextUtil.makeDependencies(groups.get("condition"));
            } else {
                parsedSentence = AnalyzeTextUtil.makeDependencies(sentence);
            }

            Set<Node> checkedNodes = new HashSet<>();
            for (Word word : parsedSentence.getVerbs()) {
                for (Word actor : word.getBond("АКТЁР")) {
                    Node node;
                    Line line;
                    if (condition != null) {
                        line = new Line(parsedSentence, condition);
                    } else {
                        line = new Line(parsedSentence);
                    }
                    String name = actor.getLemmas().get(0);
                    Optional<Node> match = nodes.stream().filter(n -> n.matchActor(actor)).findFirst();
                    node = match.orElse(new Node(name, actor));
                    if (checkedNodes.contains(node)) {
                        continue;
                    }
                    checkedNodes.add(node);
                    node.addLine(line);
                    if (match.isEmpty()) {
                        nodes.add(node);
                    }
                }
            }
        }
    }
    public static void compileAll() {
        for (Node node : nodes) {
            System.out.printf("New node: \"%s\"%n", node.getName());
            node.compile();
        }
    }
}
