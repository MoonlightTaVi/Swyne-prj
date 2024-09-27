package swyne.utils;

import com.github.demidko.aot.WordformMeaning;
import main.Main;
import swyne.Sentence;
import swyne.Word;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        System.out.println(new Word("который", 0));
        //System.out.println(MakeDependencyTree("Может бить ногой").getVerbs());
        //System.out.println(makeDependencies("Длина может быть больше 50 или меньше 50, длина может быть больше 30").getActors().get(1).compareLemmas(makeDependencies("Длина больше 50").getActors().get(0)));
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
                System.err.printf("Unexpected error happened at swyne.utils.AnalyzeTextUtil.MakeDependencyTree with the sentence \\\"%s\\\".\", String.join(\"\", tokens))%n", e.getMessage());
            }
        }
        //pt.makeStamp("Tokens list has been made.");
        joinedMembers = String.join("", tokensToMembers);
        //System.out.println(joinedMembers);
        String newMembers = SentenceRegexParser.parse(joinedMembers, words);
        //pt.makeStamp("swyne.utils.SentenceRegexParser.Parse has finished.");
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

}
