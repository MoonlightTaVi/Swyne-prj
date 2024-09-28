package swyne.core;


import main.Main;
import swyne.Word;

import java.util.*;

public class Core {
    public static Map<String, Node> nodes = new HashMap<>();
    public static void setup() {
        nodes.put("величина", new Value("величина"));
        nodes.put("сущность", new Node("сущность"));
    }

    public static Node getNode(Word word) {
        if (word.isCoreNode()) {
            for (Map.Entry<String, Node> entry : nodes.entrySet()) {
                if (Main.compareLists(List.of(entry.getKey()), word.getLemmas()) > 0) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}
