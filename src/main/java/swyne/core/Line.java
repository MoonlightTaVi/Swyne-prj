package swyne.core;

import swyne.Sentence;

public class Line {
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
