package swyne;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import regex.RegexUtils.*;
import swyne.AnalyzeTextUtil.*;
import static regex.RegexUtils.compileRegex;

// SentenceRegexParser collects word patterns from parser_regex.txt
//  and scans the sentence for matches with this patterns
// It highly uses regex
public class SentenceRegexParser {
    //private static final Map<String, Set<String>> dependencies = new HashMap<>();
    // "patterns" contain the key-value pairs, where the key depicts the conditions
    //  of the dependency and the main word in the processed collocation
    //  and the value is the collocation itself
    // If a collocation in the text (sentence) matches the "member of sentence" pattern,
    //  as well as the conditions (morphology between matched words, e.g. cases, numbers, genders, etc.),
    //  it is then replaced with the main word, at the time the words are connected with each other
    //  by a <<bond>> (look at swyne.AnalyzeTextUtil.Word), and the new sentence (with the collocation
    //  replaced by a single word) is recursively processed from the beginning
    // Pattern order is significant, so we use List of records instead of a Map
    private static final List<ParserPair> patterns = new ArrayList<>();
    private static final List<String> log = new ArrayList<>();

    // For testing
    public static void main(String[] args) {
        loadPatterns();
        // Put your testing code here
        System.out.println(patterns);
    }
    // Loads the patterns from the parser_regex.txt
    public static void loadPatterns() {
        // We scan the lines of the mentioned file
        try (Scanner scanner = new Scanner(new File("src/main/resources/data/parser_regex.txt"))) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                /*RegexHelper dependencyMatcher = new RegexHelper("(?<mainWord>[а-яА-Я]+)\\s*->\\s*(?<dependentWord>[а-яА-Я]+)(^[а-яА-Я])*")
                        .match(line);
                if (dependencyMatcher.find()) {
                    String key = dependencyMatcher.group("mainWord");
                    String value = dependencyMatcher.group("dependentWord");
                    if (dependencies.containsKey(key)) {
                        dependencies.get(key).add(value);
                        if (key.contains("или")) { // Is it needed? Or do they share it?
                            String[] split = key.split("или");
                            dependencies.get(String.format("%sили%s", split[1], split[0])).add(value);
                        }
                    }
                    else {
                        Set<String> newSet = new HashSet<>();
                        newSet.add(value);
                        dependencies.put(key, newSet);
                        if (key.contains("или")) {
                            String[] split = key.split("или");
                            dependencies.put(String.format("%sили%s", split[1], split[0]), newSet);
                        }
                    }
                }*/
                // mainRegex divides the line into the key and the value for the "patterns" (see explanation above)
                // Spaces near "=" sign don't matter, commentaries are possible in the text file
                RegexHelper mainRegex = new RegexHelper("^(?<key>(.*?))\\s*=\\s*(?<value>(.*?))((?=\\s*//)|$)")
                        .match(line);
                if (mainRegex.find()) {
                    String value = mainRegex.group("value");
                    // We need the following split to c
                    String[] parts = value.split(" ");
                    List<String> resultRegexList = new ArrayList<>();
                    // The "keyword" is simply a set of possible members of sentence of a word in the text,
                    //  connected with "или" if there are several of them. The order doesn't matter.
                    // The "info" is a temporary group, what matters is the "infoInterior", which
                    //  represents the morphology information (case, gender, number) the word in the text
                    //  should contain. It is used when e.g. the words are dependent only in an exact form.
                    String regex = "([^а-яА-Я]*)(?<keyword>[а-яА-Я]+)(?<info><(?<infoInterior>.*?)>)([^а-яА-Я]*)";
                    for (Entry matcher : compileRegex(regex).match(value)) {
                        // We split the "keyword" into separate members of sentence
                        String[] split = matcher.group("keyword").split("или");
                        // "id" represents the position of the word in the collocation, which
                        //  is absent in the file.
                        int id = resultRegexList.size();
                        // We transform the "ЧЛЕНилиДРУГОЙЧЛЕН<МОРФОЛОГИЯ>" to "(?<word*ID*>(ЧЛЕН|ДРУГОЙЧЛЕН)(или(ЧЛЕН|ДРУГОЙЧЛЕН))-(?<wordId*ID*>[0-9]+)(?<info*ID*><МОРФОЛОГИЯ>))
                        //  where "wordId" represents the position of the ACTUAL word in the ACTUAL sentence (during code compilation)
                        // We also replace the text and the morphology with that, the punctuation stays untouched.
                        // If there are two or more possible members of sentence
                        if (split.length > 1) {
                            resultRegexList.add(parts[id].replace(matcher.group("keyword"), String.format("(?<word%d>(%s)(или(%s))*-(?<wordId%d>[0-9]+)", id, String.join("|", split), String.join("|", split), id)).replace(matcher.group("info"), String.format("(?<info%d><%s>))", id, matcher.group("infoInterior"))));
                        }
                        // If there's only one variation
                        else {
                            resultRegexList.add(parts[id].replace(matcher.group("keyword"), String.format("(?<word%d>((?:или[А-Яа-я]+)*%s(?:или[А-Яа-я]+)*)-(?<wordId%d>[0-9]+)", id, split[0], id)).replace(matcher.group("info"), String.format("(?<info%d><%s>))", id, matcher.group("infoInterior"))));
                        }
                    }
                    // We then collect the transformed groups into a single group "collocation", which is needed further.
                    String resultRegex = String.format("(.*?)(?<collocation>%s)(.*?)", String.join("\\s", resultRegexList));
                    patterns.add(new ParserPair(mainRegex.group("key"), resultRegex));
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("swyne.SentenceRegexParser has no access to the \"parser_regex.txt\" file.");
        }
    }
    // The magic of NLP lays here
    public static String parse(String text, AnalyzeTextUtil.Word[] arrayOfWords) {
        if (patterns.isEmpty()) {
            System.err.println("The \"patterns\" field in swyne.SentenceRegexParser is empty.");
            return "";
        }
        log.add(String.format("New iteration: \"%s\"%n", text));
        // We go through all the patterns in the given order one by one
        for (ParserPair entry : patterns) {
            // We first need to pop the morphology "info" information out of the collocation,
            //  since the sentence (made of sentence members, not the actual words) does not contain it
            String regexRaw = entry.value;
            String regex = regexRaw;
            String modifyRegex = "(?<match>(?<member>\\(\\?<word([0-9]+)>([а-яА-Я()|*?:\\[\\]\\-+]+)-\\(\\?<wordId([0-9]+)>\\[0-9]\\+\\))\\(\\?<info(?<id>([0-9]+))><(?<info>[а-яА-Я,]*)>\\)\\))";
            Map<String, String> id2info = new HashMap<>();
            for (Entry modifyMatcher : compileRegex(modifyRegex).match(regexRaw)) {
                regex = regex.replace(modifyMatcher.group("match"), modifyMatcher.group("member") + ")");
                id2info.put(modifyMatcher.group("id"), modifyMatcher.group("info"));
            }
            // We then start the work
            for (Entry matcher : compileRegex(regex).match(text)) {
                // We process the key of the pattern, extracting...
                RegexHelper keyMatcher = new RegexHelper("(?<dependency>[а-яА-Я-:Ёё]+):\\s*(?<member>[а-яА-Я]+(\\((.*?)\\))?)\\[(?<info>(.*?))]")
                        .match(entry.key);
                String dependency = ""; // Type of word to word dependency (e.g. ДЕЙСТВИЕ-АКТЁР)
                String mainMember = ""; // Main member (e.g. СКАЗУЕМОЕ) with the info about its morphology
                String infoToMatch = ""; // The morphology the matched words should share
                if (keyMatcher.find()) {
                    dependency = keyMatcher.group("dependency");
                    mainMember = keyMatcher.group("member");
                    infoToMatch = keyMatcher.group("info");
                }
                // We collect a list of (position of the word in the sentence)-(word)
                List<ParserPair> id2word = new ArrayList<>();
                String memberRegex = "(^[а-яА-Я])*?(?<member>[а-яА-Я]+)-(?<memberId>[0-9]+)(^[а-яА-Я])*?";
                boolean matchInvalid = false;
                for (Entry memberMatcher : compileRegex(memberRegex).match(matcher.group("collocation"))) {
                    String id = memberMatcher.group("memberId");
                    // If the word does not meet the conditions of morphology (e.g. not in the correct case)
                    if (!arrayOfWords[Integer.parseInt(id)].matchInfo(id2info.get(id))) {
                        matchInvalid = true; // We skip this match - it is NOT a match
                        break;
                    } // Else we add it for further processing
                    id2word.add(new ParserPair(id, memberMatcher.group("member")));
                }
                if (matchInvalid) {
                    continue;
                }

                if (id2word.size() == 3) { // If the collocation consists of three words
                    Word origin = arrayOfWords[Integer.parseInt(id2word.get(1).key)];
                    Word direction1 = arrayOfWords[Integer.parseInt(id2word.get(0).key)];
                    Word direction2 = arrayOfWords[Integer.parseInt(id2word.get(2).key)];
                    int preferableDirectionId = 2; // We need to connect the middle one with ONE of the sides, preferably right
                    Word preferableDirection = direction2;
                    // The replaceRegex finds the two given words in the sentence and all the punctuation between them
                    String replaceRegex = String.format("(?<match>%s-%s(.*?)%s-%s)", id2word.get(1).value, id2word.get(1).key, id2word.get(2).value, id2word.get(2).key);
                    // If the left direction turns to be preferable...
                    if (origin.matchWith(direction1, infoToMatch) > origin.matchWith(direction2, infoToMatch)) {
                        preferableDirectionId = 0;
                        preferableDirection = direction1;
                        replaceRegex = String.format("(?<match>%s-%s(.*?)%s-%s)", id2word.get(0).value, id2word.get(0).key, id2word.get(1).value, id2word.get(1).key);
                    } else if (origin.matchWith(direction2, infoToMatch) == 0) { // Or no matches between words by morphology at all
                        continue; // Skip it
                    }
                    // Compare the words with "mainMember" from before
                    String main = findMainMember(id2word.get(preferableDirectionId).value, preferableDirection, id2word.get(1).value, origin, mainMember);
                    String mainId = "";
                    if (id2word.get(1).value.equals(main)) { // If the middle one is the main
                        mainId = id2word.get(1).key;
                        log.add(String.format("%s: \"%s\" -> \"%s\"%n", dependency, origin.getName(), preferableDirection.getName()));
                        origin.makeBond(preferableDirection, dependency);
                    }
                    else if (id2word.get(preferableDirectionId).value.equals(main)) { // If the side is the main
                        mainId = id2word.get(preferableDirectionId).key;
                        log.add(String.format("%s: \"%s\" -> \"%s\"%n", dependency, preferableDirection.getName(), origin.getName()));
                        preferableDirection.makeBond(origin, dependency);
                    }
                    if (compileRegex(replaceRegex).match(text).find()) { // Finally, we replace the two with one
                        String forFurtherProcessing = replacePairWithOne(text, matcher.group("collocation"), main, mainMember, mainId);
                        //System.out.println(forFurtherProcessing);
                        // And we recursively start from the beginning, given a new sentence for processing
                        String ret = parse(forFurtherProcessing, arrayOfWords);
                        if (ret.isEmpty()) { // If the job is finished, return the unprocessed (at this iteration) text
                            return forFurtherProcessing;
                        } else {
                            return ret;
                        }
                    }
                }
                else if (id2word.size() == 2) { // If there is only a pair of words, it's simpler
                    Word first = arrayOfWords[Integer.parseInt(id2word.get(0).key)];
                    Word second = arrayOfWords[Integer.parseInt(id2word.get(1).key)];
                    if (first.matchWith(second, infoToMatch) > 0.0f) { // If there's a mere chance of matching
                        // We do the same
                        String main = findMainMember(id2word.get(0).value, first, id2word.get(1).value, second, mainMember);
                        String mainId = "";
                        // First make corresponding bonds
                        if (id2word.get(0).value.equals(main)) {
                            mainId = id2word.get(0).key;
                            log.add(String.format("%s: \"%s\" -> \"%s\"%n", dependency, first.getName(), second.getName()));
                            first.makeBond(second, dependency);
                        }
                        else if (id2word.get(1).value.equals(main)) {
                            mainId = id2word.get(1).key;
                            log.add(String.format("%s: \"%s\" -> \"%s\"%n", dependency, second.getName(), first.getName()));
                            second.makeBond(first, dependency);
                        }
                        // Then replace
                        String replaceRegex = String.format("(?<match>%s-%s(.*?)%s-%s)", id2word.get(0).value, id2word.get(0).key, id2word.get(1).value, id2word.get(1).key);
                        if (compileRegex(replaceRegex).match(text).find()) {
                            String forFurtherProcessing = replacePairWithOne(text, matcher.group("collocation"), main, mainMember, mainId);
                            //System.out.println(forFurtherProcessing);
                            // And start over
                            String ret = parse(forFurtherProcessing, arrayOfWords);
                            if (ret.isEmpty()) {
                                return forFurtherProcessing;
                            } else {
                                return ret;
                            }
                        }
                    }
                }
            }
        }
        log.add("Parsing complete!");
        return ""; // Or there are no matches
    }
    public static String parse(String text, AnalyzeTextUtil.Word[] arrayOfWords, String originalText) {
        log.add(String.format("New sentence: \"%s\"%n", originalText));
        String result = parse(text, arrayOfWords);
        log.forEach(System.out::println);
        log.clear();
        return result;
    }
    // Divide the main member from the key into word and morphology
    private static Map<String, String> splitMainIntoParts(String main) {
        Map<String, String> res = new HashMap<>();
        String regex = "(?<main>[а-яА-Я]+)(\\((?<morph>.*)\\))*";
        for (Entry matcher : compileRegex(regex).match(main)) {
            res.put("main", matcher.group("main"));
            res.put("morph", matcher.group("morph"));
        }
        //System.out.println(res);
        return res;
    }
    // Find the main word between two (e.g. "СКАЗУЕМОЕ" is main to "ПОДЛЕЖАЩЕЕ")
    private static String findMainMember(String a, Word aW, String b, Word bW, String matchWith) {
        Map<String, String> groups = splitMainIntoParts(matchWith);
        //System.out.println(matchWith);
        String main = groups.get("main");
        String morph = groups.get("morph");
        String[] split = main.split("или");
        for (String match : split) {
            if (a.contains(match)) { // If it's the a/aW word
                if (morph == null || aW.matchInfo(morph)) { // If the morphology corresponds (if there are two words of the same PoS, but different morphology)
                    return a;
                }
            } else if (b.contains(match)) { // If it's the b/bW word
                if (morph == null || bW.matchInfo(morph)) {
                    return b;
                }
            }
        }
        System.err.printf("Members of sentence \"%s\" (\"%s\") and \"%s\" (\"%s\") don't belong to \"%s\".%n", aW.getName(), a, bW.getName(), b, matchWith);
        return "42";
    }
    // Replace collocation with the main word
    private static String replacePairWithOne(String sourceText, String pair, String main, String replaceWithRaw, String newMemberId) {
        Set<String> matches = new HashSet<>();
        String replaceWith = splitMainIntoParts(replaceWithRaw).get("main");
        String[] mainSplit = main.split("или"); // We need to filter the possible members to these which correspond the main
        String[] replaceSplit = replaceWith.split("или");
        for(String mainPart : mainSplit) { // For condition possible members
            for(String replacePart : replaceSplit) { // For the main (int hte sentence) possible members
                if (mainPart.equals(replacePart)) {
                    matches.add(mainPart); // If they are equal, we filter them in
                }
            }
        }
        // And return the replaced text
        return sourceText.replace(pair, String.join("или", matches) + "-" + newMemberId);
    }
    // Used for "patterns"
    private record ParserPair(String key, String value) {
    }
}
