package swyne;

import com.github.demidko.aot.MorphologyTag;
import com.github.demidko.aot.WordformMeaning;
import main.Main;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnalyzeTextUtil {
    public static void main(String[] args) {
        Main.setup();

        //System.out.println(testTransformations("что"));
        //System.out.println(new WordRole("больного", 0).matchWith(new WordRole("сильный", 1), "п,р,ч"));
        //System.out.println(new WordRole("воин", 0).matchWith(new WordRole("сильный", 1), "п,р,ч"));
        //System.out.println(new WordRole("стул", 0).matchWith(new WordRole("сильный", 1), "р,ч,п"));
        //.out.println(new WordRole("слабого", 0));
        //System.out.println(new WordRole("по", 0));
        //MakeDependencyTree("Сильный, могучий воин бьёт слабого бандита");
        //MakeDependencyTree("Длина есть величина. Длина лежит от 0 до 100.");
        //MakeDependencyTree("Воин сильный больного");
        //System.out.println(new WordRole("больше", 0));
        //System.out.println(MakeDependencyTree("Может бить ногой").getVerbs());
        System.out.println(makeDependencies("Длина может быть больше 50 или меньше 50, длина может быть больше 30").getActors().get(1).compareLemmas(makeDependencies("Длина больше 50").getActors().get(0)));
    }

    // Prepares the text for further analysis by dividing it into separate tokens
    // Receives text of any sentence count, returns a List of tokens
    public static List<String> splitIntoTokens(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder lastToken = new StringBuilder();
        for(char c : text.toCharArray()) { // Iterate through the characters in the text
            // If it's a punctuation sign
            if (!Character.isLetterOrDigit(c) && !Character.isSpaceChar(c)) {
                if (!lastToken.isEmpty()) { // If the last/current token is NOT empty
                    // Punctuation signs go together into one token
                    if ((!Character.isLetterOrDigit(lastToken.charAt(lastToken.length() - 1)) &&
                            !Character.isSpaceChar(lastToken.charAt(lastToken.length() - 1))) ||
                            // '-' between letters also joins them together
                            (c == '-' && Character.isLetter(lastToken.charAt(lastToken.length() - 1)))) {
                        lastToken.append(c);
                    }
                    // Or else, we start a new token
                    else {
                        tokens.add(lastToken.toString());
                        lastToken.setLength(0);
                        lastToken.append(c);
                    }
                }
                // If the last token was empty, we start it
                else {
                    lastToken.append(c);
                }
            }
            // The character in iteration is a letter or a digit
            else if (Character.isLetterOrDigit(c)) {
                if (!lastToken.isEmpty()) {
                    // As before, the characters of the same type go together
                    if (Character.isLetterOrDigit(lastToken.charAt(lastToken.length() - 1))) {
                        lastToken.append(c);
                    } else { // Or we start a new token
                        tokens.add(lastToken.toString());
                        lastToken.setLength(0);
                        lastToken.append(c);
                    }
                }
                else { // If the last token was empty
                    lastToken.append(c);
                }
            }
            // If the character is a space character
            else if (Character.isSpaceChar(c)) {
                if (!lastToken.isEmpty()) {
                    // Unlike before, spaces don't go together
                    // There can be only ONE space token, dividing other tokens
                    if (!Character.isSpaceChar(lastToken.charAt(lastToken.length() - 1))) {
                        tokens.add(lastToken.toString());
                        lastToken.setLength(0);
                        lastToken.append(c);
                    }
                }
                else {
                    lastToken.append(c);
                }
            }
        }
        // After we finished the iteration, we need to add the last token
        // (usually the token appending happens on starting a new token, but here it is the end of the text, so...)
        if (!lastToken.isEmpty()) {
            tokens.add(lastToken.toString());
        }
        return tokens;
    }

    public static Sentence makeDependencies(String text) {
        //utils.PassedTime pt = new utils.PassedTime("MakeDependencyTree");
        List<String> tokens = splitIntoTokens(text);
        //pt.makeStamp("splitIntoTokens");

        //Graph<WordRole, Word2WordEdge> tree = new SimpleGraph<>(Word2WordEdge.class);
        String[] tokensToMembers = new String[tokens.size()];
        Word[] words = new Word[tokens.size()];
        String joinedMembers = "";
        for(int i = 0; i < tokens.size(); i++) {
            try {
                //utils.PassedTime wPT = new utils.PassedTime(String.format("Word creation: \"%s\"", tokens.get(i)));
                Word newWord = new Word(tokens.get(i), i);
                //wPT.makeStamp("Finished initialization");
                Set<String> possibleMember = newWord.getPossibleMembers();
                //wPT.makeStamp("Got possible members");
                tokensToMembers[i] = String.join("или", possibleMember) + "-" + i;
                words[i] = newWord;
            } catch(RuntimeException e) {
                tokensToMembers[i] = tokens.get(i);
                //System.err.printf("Could not find the word '%s' in the demidko.aot library.%n", tokens.get(i));
            } catch(Exception e) {
                System.err.printf("Unexpected error happened at swyne.AnalyzeTextUtil.MakeDependencyTree with the sentence \\\"%s\\\".\", String.join(\"\", tokens))%n", e.getMessage());
            }
        }
        //pt.makeStamp("Tokens list has been made.");
        joinedMembers = String.join("", tokensToMembers);
        //System.out.println(joinedMembers);
        String newMembers = SentenceRegexParser.parse(joinedMembers, words);
        //pt.makeStamp("swyne.SentenceRegexParser.Parse has finished.");
        //System.out.println("\nЗависимости между словами:\n");
        //for (WordRole word : words) {
            //if (word != null)
            //{System.out.println(word);}
        //}
        List<Word> ret = new ArrayList<>();
        //System.out.println(newMembers);
        String regex = "([а-яА-Я]+)-(?<id>[0-9]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(newMembers);
        while (matcher.find()) {
            ret.add(words[Integer.parseInt(matcher.group("id"))]);
        }
        return new Sentence(ret);
    }

    public static String testTransformations(String text) {
        List<WordformMeaning> meanings = lookupForMeanings(text);
        if (meanings.isEmpty()) {
            return String.format("Неизвестное слово: \"%s\"", text);
        }
        StringBuilder temp = new StringBuilder();
        for (WordformMeaning meaning : meanings) {
            temp.append(String.format("Слово: \"%s\", форма: %s, начальная форма: \"%s\".\n", text, meaning.getMorphology(), meaning.getLemma()));
            temp.append("Трансформации:\n");
            for (WordformMeaning t : meaning.getTransformations()) {
                temp.append(String.format("%s %s\n", t.toString(), t.getMorphology()));
            }
        }
        return temp.toString();
    }

    public static class Word {
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
            for(WordformMeaning meaning : meanings) {
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
            return lemmas;
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
                                    if (!getBonds("АКТЁР").isEmpty() && !toWord.getBonds("АКТЁР").isEmpty()) {
                                        //System.out.println(getBonds("АКТЁР"));
                                        //System.out.println(111);
                                        //System.out.println(toWord.getBonds("АКТЁР"));
                                        return 0.0f;
                                    }
                                    break;
                                }
                                case "ОСН": {
                                    if (!getBonds("ОСН").isEmpty() && !toWord.getBonds("ОСН").isEmpty()) {
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
                for (Word actor : getBonds("АКТЁР")) {
                    actor.addBond(toWord, "ДЕЙСТВИЕ");
                    toWord.addBond(actor, "АКТЁР");
                }
            }
        }
        public Set<Word> getBonds(String bond) {
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
            return getBonds(bondName).stream().findFirst();
        }
        public Word getFirstBondWithCondition(String bondName, WordRoleCondition condition) {
            return getBondsWithCondition(bondName, condition).stream().findFirst().orElse(null);
        }
        public Word getNextVerb() {
            if (getBonds("СЛЕДУЕТ") == null) {
                return null;
            }
            Optional<Word> next = getBonds("СЛЕДУЕТ").stream().findFirst();
            return next.orElse(null);
        }
        public Set<String> getAllBondLemmas() {
            return getAllBondLemmas(new HashSet<>());
        }
        public Set<String> getAllBondLemmas(Set<Word> ignore) {
            Set<String> ret = new HashSet<>(lemmas);
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
            Set<String> ret = new HashSet<>(lemmas);
            ret.addAll(checked);
            for (Map.Entry<String, Set<Word>> entry : bonds.entrySet()) {
                for (Word word : entry.getValue()) {
                    if (!ret.contains(word.getLemmas().get(0)) && !ignore.contains(word)) {
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
        @Override
        public String toString() {
            StringBuilder info = new StringBuilder();
            for(Set<String> morphology : morphologies) {
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
    public interface WordRoleCondition extends Predicate<Word> {
        @Override
        public boolean test(Word word);
    }
    public static class Sentence {
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

        @Override
        public String toString() {
            List<String> ret = new ArrayList<>();
            for (Word word : words) {
                ret.add(word.toString());
            }
            return String.join("\n", ret);
        }
    }
    public static class BondCollector {
        private final Word start;
        private List<Word> currentPoint = new ArrayList<>();
        private final Map<String, List<Word>> waypoints = new HashMap<>();
        private List<Word> collection = new ArrayList<>();
        public BondCollector(Word word) {
            start = word;
            currentPoint.add(word);
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
                Set<Word> bonds = word.getBonds(bond);
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
                Set<Word> bonds = word.getBonds(bond);
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
}
