package swyne;

import java.util.*;

public class BondCollector {
    private final Word start;
    private List<Word> currentPoint = new ArrayList<>();
    private final Map<String, List<Word>> waypoints = new HashMap<>();
    private List<Word> collection = new ArrayList<>();

    public BondCollector(Word word) {
        start = word;
        if (word != null) {
            currentPoint.add(word);
        }
    }

    public BondCollector toStart() {
        currentPoint.clear();
        currentPoint.add(start);
        waypoints.clear();
        return this;
    }

    public BondCollector goTo(String bond) {
        if (currentPoint.isEmpty()) {
            return this;
        }
        List<Word> temp = new ArrayList<>();
        for (Word word : currentPoint) {
            Set<Word> bonds = word.getBond(bond);
            if (bonds != null) {
                temp.addAll(bonds);
            }
        }
        currentPoint = temp;
        waypoints.put(bond, temp);
        return this;
    }

    public BondCollector goBackTo(String waypoint) {
        if (waypoints.containsKey(waypoint)) {
            currentPoint = waypoints.get(waypoint);
        }
        return this;
    }

    public BondCollector collect() {
        if (currentPoint.isEmpty()) {
            return this;
        }
        collection = currentPoint;
        return this;
    }

    public BondCollector collect(String bond) {
        if (currentPoint.isEmpty()) {
            return this;
        }
        List<Word> temp = new ArrayList<>();
        for (Word word : currentPoint) {
            Set<Word> bonds = word.getBond(bond);
            temp.addAll(bonds);
        }
        if (!temp.isEmpty()) {
            collection = temp;
        }
        return this;
    }

    public BondCollector filter(WordRoleCondition condition) {
        if (currentPoint.isEmpty()) {
            return this;
        }
        currentPoint = currentPoint.stream().filter(condition).toList();
        return this;
    }

    public Optional<Word> getFirst() {
        Optional<Word> ret = Optional.empty();
        if (!collection.isEmpty()) {
            ret = Optional.of(collection.get(0));
        }
        return ret;
    }

    public List<Word> get() {
        return collection;
    }
}
