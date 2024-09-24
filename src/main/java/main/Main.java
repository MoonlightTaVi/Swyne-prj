package main;//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import swyne.utils.CommandParser;
import swyne.core.Core;
import swyne.utils.SentenceRegexParser;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConsoleWindow window = new ConsoleWindow();
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        });
    }
    public static<T> int compareLists(List<T> a, List<T> b) {
        int matches = 0;
        for (T itemA : a) {
            if (b.contains(itemA)) {
                matches++;
            }
        }
        return matches;
    }
    public static<T> Set<T> matchSets(Set<T> a, Set<T> b) {
        Set<T> ret = new HashSet<>();
        for (T itemA : a) {
            if (b.contains(itemA)) {
                ret.add(itemA);
            }
        }
        return ret;
    }
    public static void setup() {
        SentenceRegexParser.loadPatterns();
        // The first lookupForMeanings, which is located at each WordRole initialization, takes a lot of time
        //  for some reason. Then, independently of position in the code, it is very fast.
        //  So, we have to make a single call in the beginning of the program to increase the runtime performance.
        lookupForMeanings("инициализация");
        Core.setup();
        CommandParser.setup();
    }
}