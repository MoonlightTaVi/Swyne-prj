package swyne;

import com.github.demidko.aot.MorphologyTag;
import com.github.demidko.aot.WordformMeaning;
import main.Main;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

public class Word {
    private String name;
    private final List<String> lemmas = new ArrayList<>();
    private int id = 0;
    private double number;
    private final List<Set<String>> morphologies = new ArrayList<>();
    private final Map<String, Set<Word>> bonds = new HashMap<>();

    public Word(String word, int id) throws RuntimeException {
        List<WordformMeaning> meanings = lookupForMeanings(word);
        if (meanings.isEmpty()) {
            try {
                number = Double.parseDouble(word);
                name = word;
                morphologies.add(new HashSet<>(List.of("ЧИСЛО")));
                lemmas.add(word);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException();
            }
        }
        name = word;
        this.id = id;
        for (WordformMeaning meaning : meanings) {
            Set<String> morphology = meaning.getMorphology().stream()
                    .map(MorphologyTag::toString).collect(Collectors.toSet());
            lemmas.add(meaning.getLemma().toString());
            morphologies.add(morphology);
        }
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public double getNum() {
        return number;
    }

    public List<Set<String>> findMorphology(String[] tags) {
        List<Set<String>> ret = new ArrayList<>();
        for (Set<String> morphology : morphologies) {
            Set<String> temp = new HashSet<>();
            for (String form : morphology) {
                for (String tag : tags) {
                    if (form.equals(tag)) {
                        temp.add(tag);
                    }
                }
            }
            ret.add(temp);
        }
        return ret;
    }

    public Set<String> findMorphologyJoined(String[] tags) {
        Set<String> ret = new HashSet<>();
        for (Set<String> morphology : morphologies) {
            for (String form : morphology) {
                for (String tag : tags) {
                    String[] split = form.split("-");
                    for (String part : split) {
                        if (part.equals(tag) || tag.equals("...")) {
                            ret.add(part);
                        }
                    }
                }
            }
        }
        return ret;
    }
    public List<String> getLemmas() {
        return getLemmas(false);
    }
    public List<String> getLemmas(boolean includePronouns) {
        List<String> ret = new ArrayList<>(lemmas);
        if (includePronouns) {
            if (getPOSJoined().contains("С")) {
                if (getGenderJoined().contains("мр")) {
                    ret.add("он");
                }
                if (getGenderJoined().contains("жр")) {
                    ret.add("она");
                }
                if (getGenderJoined().contains("ср")) {
                    ret.add("оно");
                }
            }
        }
        return ret;
    }

    public List<Set<String>> getPOS() {
        return findMorphology(new String[]{"С", "Г", "П", "Н", "ПРИЧАСТИЕ", "КР_ПРИЧАСТИЕ", "ДЕЕПРИЧАСТИЕ", "ПРЕДЛ", "МС", "СОЮЗ", "ИНФИНИТИВ", "ЧИСЛО", "ЧИСЛ"});
    }

    public Set<String> getPOSJoined() {
        return findMorphologyJoined(new String[]{"С", "Г", "П", "Н", "ПРИЧАСТИЕ", "КР_ПРИЧАСТИЕ", "ДЕЕПРИЧАСТИЕ", "ПРЕДЛ", "МС", "СОЮЗ", "ИНФИНИТИВ", "ЧИСЛО", "ЧИСЛ"});
    }

    public List<Set<String>> getGender() {
        return findMorphology(new String[]{"мр", "жр", "ср"});
    }
    public Set<String> getGenderJoined() {
        return findMorphologyJoined(new String[]{"мр", "жр", "ср"});
    }

    public List<Set<String>> getNumber() {
        return findMorphology(new String[]{"ед", "мн"});
    }

    public Set<String> getNumberJoined() {
        return findMorphologyJoined(new String[]{"ед", "мн"});
    }

    public List<Set<String>> getPerson() {
        return findMorphology(new String[]{"1л", "2л", "3л"});
    }

    public Set<String> getPersonJoined() {
        return findMorphologyJoined(new String[]{"1л", "2л", "3л"});
    }

    public List<Set<String>> getTime() {
        return findMorphology(new String[]{"прш", "нст", "буд"});
    }

    public Set<String> getTimeJoined() {
        return findMorphologyJoined(new String[]{"прш", "нст", "буд"});
    }

    public List<Set<String>> getComparison() {
        return findMorphology(new String[]{"сравн", "прев"});
    }

    public List<Set<String>> getCase() {
        return findMorphology(new String[]{"им", "рд", "дт", "вн", "тв", "пр"});
    }

    public Set<String> getCasesJoined() {
        return findMorphologyJoined(new String[]{"им", "рд", "дт", "вн", "тв", "пр"});
    }

    public List<Set<String>> getVerbForm() {
        return findMorphology(new String[]{"ИНФИНИТИВ", "стр", "пвл"});
    }

    public boolean isQuestion() {
        return !findMorphologyJoined(new String[]{"вопр"}).isEmpty();
    }

    public float matchWith(Word toWord, String matchBy) {
        if (matchBy.isEmpty()) {
            return 1.0f;
        }
        float weight = 0.0f;
        String[] matcherVariations = matchBy.split("\\|");
        for (String matcherVariation : matcherVariations) {
            String[] matcherInfos = matcherVariation.split(",");
            float matches = 0.0f;
            float occurrences = 0.0f;
            for (int i = 0; i < getPOS().size(); i++) {
                for (int j = 0; j < toWord.getPOS().size(); j++) {
                    occurrences++;
                    boolean toAdd = true;
                    for (String matcherInfo : matcherInfos) {
                        switch (matcherInfo) {
                            case "л": {
                                if (Main.matchSets(getPerson().get(i), toWord.getPerson().get(j)).isEmpty()) {
                                    toAdd = false;
                                }
                                break;
                            }
                            case "ч": {
                                if (Main.matchSets(getNumber().get(i), toWord.getNumber().get(j)).isEmpty()) {
                                    toAdd = false;
                                }
                                break;
                            }
                            case "р": {
                                if (Main.matchSets(getGender().get(i), toWord.getGender().get(j)).isEmpty()) {
                                    toAdd = false;
                                }
                                break;
                            }
                            case "п": {
                                if (Main.matchSets(getCase().get(i), toWord.getCase().get(j)).isEmpty()) {
                                    toAdd = false;
                                    break;
                                }
                                break;
                            }
                            case "п+": {
                                if (Main.matchSets(getCase().get(i), toWord.getCase().get(j)).isEmpty()) {
                                    toAdd = false;
                                    break;
                                }
                                Set<String> joinedCases = new HashSet<>();
                                joinedCases.addAll(getCasesJoined());
                                joinedCases.addAll(toWord.getCasesJoined());
                                if (Main.matchSets(joinedCases, Stream.of(new String[]{"им", "вн"}).collect(Collectors.toSet())).size() >= 2) {
                                    if (!name.toLowerCase().equals(lemmas.get(i)) || !toWord.name.toLowerCase().equals(toWord.lemmas.get(j))) {
                                        toAdd = false;
                                    }
                                }
                                break;
                            }
                            case "в": {
                                if (Main.matchSets(getTime().get(i), toWord.getTime().get(j)).isEmpty()) {
                                    toAdd = false;
                                }
                                break;
                            }
                            case "АКТ": {
                                if (!getBond("АКТЁР").isEmpty() && !toWord.getBond("АКТЁР").isEmpty()) {
                                    //System.out.println(getBonds("АКТЁР"));
                                    //System.out.println(111);
                                    //System.out.println(toWord.getBonds("АКТЁР"));
                                    return 0.0f;
                                }
                                break;
                            }
                            case "ОСН": {
                                if (!getBond("ОСН").isEmpty() && !toWord.getBond("ОСН").isEmpty()) {
                                    return 0.0f;
                                }
                                break;
                            }
                            default: {
                                System.err.printf("The \"%s\" morphology information piece is not processed at WordRole.matchWith().%n", matcherInfo);
                            }
                        }
                    }
                    if (toAdd) {
                        matches++;
                    }
                }
            }
            float newWeight = matches / occurrences;
            if (newWeight < weight || weight == 0.0f) {
                weight = newWeight;
            }
        }
        return weight;
    }

    public boolean matchInfo(String infoString) {
        if (infoString == null || infoString.isEmpty()) {
            return true;
        }
        String[] infos = infoString.split(",");
        Set<String> morphology = findMorphologyJoined(new String[]{"..."});
        for (String info : infos) {
            boolean exclude = false;
            if (info.contains("!")) {
                info = info.replace("!", "");
                exclude = true;
            }
            if (morphology.contains(info) == exclude) {
                //System.out.println(111);
                return false;
            }
        }
        return true;
    }

    public Set<String> getPossibleMembers() {
        Set<String> possibleMembers = new HashSet<>();
        Set<String> POSs = getPOSJoined();
        if (POSs.contains("П") && POSs.contains("МС")) {
            possibleMembers.add("ПРИЛМЕСТ");
        }
        if (POSs.contains("С") || POSs.contains("МС")) {
            Set<String> cases = getCasesJoined();
            if (cases.contains("им")) {
                possibleMembers.add("ПОДЛЕЖАЩЕЕ");
            }
            possibleMembers.add("ДОПОЛНЕНИЕ");
        }
        if (POSs.contains("П") || POSs.contains("ПРИЧАСТИЕ") || POSs.contains("КР_ПРИЧАСТИЕ") || POSs.contains("ЧИСЛ")) {
            possibleMembers.add("ОПРЕДЕЛЕНИЕ");
        }
        if (POSs.contains("Г")) {
            possibleMembers.add("СКАЗУЕМОЕ");
        }
        if (POSs.contains("ИНФИНИТИВ")) {
            possibleMembers.add("ИНФИНИТИВ");
        }
        if (POSs.contains("СОЮЗ")) {
            possibleMembers.add("СОЮЗ");
        }
        if (POSs.contains("ПРЕДЛ")) {
            possibleMembers.add("ПРЕДЛ");
        }
        if (POSs.contains("ДЕЕПРИЧАСТИЕ")) {
            possibleMembers.add("ДЕЕПРИЧАСТИЕ");
        }
        if (POSs.contains("ЧИСЛО")) {
            possibleMembers.add("ЧИСЛО");
        }
        return possibleMembers;
    }

    public void addBond(Word toWord, String dependencyType) {
        if (this == toWord) {
            return;
        }
        if (bonds.containsKey(dependencyType)) {
            bonds.get(dependencyType).add(toWord);
        } else {
            Set<Word> newBond = new HashSet<>();
            newBond.add(toWord);
            bonds.put(dependencyType, newBond);
        }
    }

    public void makeBond(Word toWord, String dependencyType) {
        String[] dependencySplit = dependencyType.split("-");
        addBond(toWord, dependencySplit[1]);
        toWord.addBond(this, dependencySplit[0]);
        if (toWord.bonds.containsKey("ОДНОРОДНОЕ")) {
            for (Word homogenous : toWord.bonds.get("ОДНОРОДНОЕ")) {
                addBond(homogenous, dependencySplit[1]);
                homogenous.addBond(this, dependencySplit[0]);
            }
        }
        if (dependencySplit[1].equals("СЛЕДУЕТ")) {
            for (Word actor : getBond("АКТЁР")) {
                actor.addBond(toWord, "ДЕЙСТВИЕ");
                toWord.addBond(actor, "АКТЁР");
            }
        }
    }

    public void refactorBond(Word toWord, String bond) {
        for (Word w : getBond(bond)) {
            toWord.addBond(w, bond);
            for (Map.Entry<String, Set<Word>> entry : w.getBonds().entrySet()) {
                if (entry.getValue().contains(this)) {
                    w.getBonds().get(entry.getKey()).remove(this);
                    w.makeBond(toWord, entry.getKey());
                }
            }
        }
    }

    public void refactorBond(Word toWord, String bond, String newBondType) {
        for (Word w : getBond(bond)) {
            toWord.makeBond(w, newBondType);
            for (Map.Entry<String, Set<Word>> entry : w.getBonds().entrySet()) {
                if (entry.getValue().contains(this)) {
                    w.getBonds().get(entry.getKey()).remove(this);
                }
            }
        }
    }

    public Map<String, Set<Word>> getBonds() {
        return bonds;
    }

    public Set<Word> getBond(String bond) {
        return bonds.get(bond) == null ? new HashSet<>() : bonds.get(bond);
    }

    public Set<Word> getBondsWithCondition(String bondName, WordRoleCondition condition) {
        Set<Word> ret = new HashSet<>();
        for (Map.Entry<String, Set<Word>> bondsSet : bonds.entrySet()) {
            if (bondsSet.getKey().equals(bondName)) {
                for (Word word : bondsSet.getValue()) {
                    if (condition.test(word)) {
                        ret.add(word);
                    }
                }
            }
        }
        return ret;
    }

    public Optional<Word> getFirstBond(String bondName) {
        return getBond(bondName).stream().findFirst();
    }

    public Word getFirstBondWithCondition(String bondName, WordRoleCondition condition) {
        return getBondsWithCondition(bondName, condition).stream().findFirst().orElse(null);
    }

    public String bondsToString() {
        Set<Word> checked = new HashSet<>();
        return bondsToString(checked, 0);
    }
    public String bondsToString(Set<Word> checked, int depth) {
        String indentation = "\t".repeat(depth);
        StringBuilder ret = new StringBuilder(indentation);
        ret.append(String.format("\"%s\"\n", name));
        if (checked.contains(this)) {
            return ret.toString();
        }
        checked.add(this);
        for (Map.Entry<String, Set<Word>> entry : getBonds().entrySet()) {
            ret.append(indentation);
            ret.append(entry.getKey());
            ret.append(":\n");
            for (Word w : entry.getValue()) {
                ret.append(w.bondsToString(checked, depth + 1));
            }
        }
        return ret.toString();
    }

    public Word getNextVerb() {
        if (getBond("СЛЕДУЕТ") == null) {
            return null;
        }
        Optional<Word> next = getBond("СЛЕДУЕТ").stream().findFirst();
        return next.orElse(null);
    }

    public Set<String> getAllBondLemmas() {
        return getAllBondLemmas(new HashSet<>());
    }

    public Set<String> getAllBondLemmas(Set<Word> ignore) {
        Set<String> ret = new HashSet<>(getLemmas(true));
        for (Map.Entry<String, Set<Word>> entry : bonds.entrySet()) {
            for (Word word : entry.getValue()) {
                if (!ignore.contains(word)) {
                    ret.addAll(word.getAllBondLemmas(ret, ignore));
                }
            }
        }
        return ret;
    }

    public Set<String> getAllBondLemmas(Set<String> checked, Set<Word> ignore) {
        Set<String> ret = new HashSet<>(getLemmas(true));
        ret.addAll(checked);
        for (Map.Entry<String, Set<Word>> entry : bonds.entrySet()) {
            for (Word word : entry.getValue()) {
                if (!ret.contains(word.getLemmas(true).get(0)) && !ignore.contains(word)) {
                    ret.addAll(word.getAllBondLemmas(ret, ignore));
                }
            }
        }
        return ret;
    }

    public boolean compareLemmas(Word compareTo) {
        return compareLemmas(compareTo, new HashSet<>());
    }

    public boolean compareLemmas(Word compareTo, Set<Word> ignore) {
        Set<String> ourLemmas = getAllBondLemmas(ignore);
        Set<String> theirLemmas = compareTo.getAllBondLemmas(ignore);
        Set<String> lesserSet = ourLemmas.size() > theirLemmas.size() ? theirLemmas : ourLemmas;
        return Main.matchSets(ourLemmas, theirLemmas).size() == lesserSet.size();
    }

    public boolean matchActor(Word matchTo) {
        if (matchTo.getPOSJoined().contains("С")) {
            return Main.compareLists(getLemmas(), matchTo.getLemmas()) > 0;
        } else if (matchTo.getPOSJoined().contains("МС")) {
            return matchWith(matchTo, "ч,р") > 0;
        }
        return false;
    }

    public boolean isCoreNode() {
        Map<String, Set<Word>> cutBonds = new HashMap<>(Map.copyOf(bonds));
        cutBonds.remove("ДЕЙСТВИЕ");
        return cutBonds.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        for (Set<String> morphology : morphologies) {
            info.append(morphology.toString());
        }
        StringBuilder dependencies = new StringBuilder("Зависимости:\n");
        if (bonds.isEmpty()) {
            dependencies.append("Нет зависимостей.\n");
        } else {
            for (Map.Entry<String, Set<Word>> entrySet : bonds.entrySet()) {
                List<String> words = entrySet.getValue().stream().map(Word::getName).toList();
                dependencies.append(String.format("%s: %s\n", entrySet.getKey(), String.join(", ", words)));
            }
        }
        return String.format("%s %s [%s]\n%s", name, lemmas, info, dependencies.toString());
    }
}
