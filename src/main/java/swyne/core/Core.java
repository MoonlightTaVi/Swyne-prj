package swyne.core;

import swyne.Word;

import java.util.*;

public class Core {
    public static Map<String, Node> nodes = new HashMap<>();
    public static void setup() {
        nodes.put("величина", new Value("величина"));
    }

}
