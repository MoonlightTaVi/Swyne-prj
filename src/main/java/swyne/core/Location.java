package swyne.core;

import swyne.Word;

public class Location extends Node {
    public Location(String name) {
        super(name);
    }

    public Location(String name, Word actor) {
        super(name, actor);
    }

}
