package swyne;

import java.util.*;

public class Sentence {
    private final List<Word> words;

    public Sentence(List<Word> words) {
        this.words = words;
    }

    public List<Word> getVerbs() {
        List<Word> ret = new ArrayList<>();
        //System.out.println(words);
        for (Word word : words) {
            if (word.getPOSJoined().contains("Г")) {
                ret.add(word);
            }
        }
        return ret;
    }

    public List<Word> getWords() {
        return words;
    }

    public List<Word> getActors() {
        List<Word> verbs = getVerbs();
        Set<Word> ret = new HashSet<>();
        if (verbs.isEmpty()) {
            for (Word word : words) {
                Set<String> morph = word.findMorphologyJoined(new String[]{"С", "им", "МС"});
                if ((morph.contains("С") || morph.contains("МС")) && morph.contains("им")) {
                    ret.add(word);
                }
            }
        } else {
            for (Word verb : verbs) {
                ret.addAll(new BondCollector(verb).collect("АКТЁР").get());
            }
        }
        return ret.stream().toList();
    }

    public Optional<Word> getActor(int id) {
        Optional<Word> ret = Optional.empty();
        List<Word> actors = getActors();
        if (actors.size() <= id) {
            return ret;
        }
        ret = Optional.of(actors.get(id));
        return ret;
    }

    public Word getNextTo(Word previousWord) {
        int id = words.indexOf(previousWord);
        if (id >= 0) {
            if (id+1 < words.size()) {
                return words.get(id+1);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        List<String> ret = new ArrayList<>();
        for (Word word : words) {
            ret.add(word.bondsToString());
        }
        return String.join("\n", ret);
    }
}
