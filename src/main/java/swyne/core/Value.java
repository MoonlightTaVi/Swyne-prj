package swyne.core;

import main.Main;
import swyne.BondCollector;
import swyne.Sentence;
import swyne.Word;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Value extends Node {
    public Value(String name) {
        super(name);
    }

    public Value(String name, Word actor) {
        super(name, actor);
    }

    public void sum(Word value, boolean positive) {
        try {
            double currentValue = Double.parseDouble(args.get("значение"));
            double sum = Double.parseDouble(value.getName());
            if (positive) {
                currentValue += sum;
            } else {
                currentValue -= sum;
            }
            args.put("значение", String.valueOf(currentValue));
        } catch (IllegalArgumentException e) {
            System.err.println("The transformation from a variable name to a double number is not realized yet.");
        }
    }

    @Override
    public boolean execute(Line line) {
        super.execute(line);
        Sentence sentence = line.getLine();
        List<Word> verbs = sentence.getVerbs();
        for (Word verb : verbs) {
            for (Word actor : verb.getBond("АКТЁР")) {
                if (!matchActor(actor)) {
                    continue;
                }
                for (Word nextVerb = verb; nextVerb != null; nextVerb = nextVerb.getNextVerb()) {
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
                            if (!args.containsKey("значение")) {
                                System.err.printf("No value has been assigned to \"%s\".%n", actor.getName());
                                continue;
                            }
                            sum(sumW, nextVerb.getLemmas().contains("увеличиться"));
                        }
                    }
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
            if (!matchActor(actor)) {
                continue;
            }
            Set<Word> comparisons = actor.getBond("СРАВН");
            if (comparisons.isEmpty()) {
                continue;
            }
            for (Word comparison : comparisons) {
                Optional<Word> number = comparison.getFirstBond("ЧЕМ");
                if (number.isPresent()) {
                    double value = 0.0;
                    try {
                        if (args.containsKey("значение")) {
                            value = Double.parseDouble(args.get("значение"));
                        }
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
