package regex;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// RegexUtils helps with easily iterating through match groups, customising .find()'s end
//  (to get both "a, b, a" and "a, c, a" at "a, b, a, c, a", which usual Matcher.find() doesn't allow)
//  and some other stuff, such as logging (both into the console and retrieving a log as an array of String)
public class RegexUtils {
    public static void main(String[] args) {
        // You can use several regex patterns at once
        RegexHelper regex = new RegexHelper("(?<fruit>(яблоко|апельсин|персик))",
                "(?<several>(?<firstWord>[а-яА-Я]+)(, ([а-яА-Я]+))*)",
                "(?<betweenApples>яблоко, (.*?), яблоко)") //                           VVV Here you can specify .find()'s step
                .match("яблоко, книга, яблоко, апельсин, яблоко", "\\bяблоко\\b").printLog(); // Also, print log at the console
        for (Entry entry : regex) { // Iterate through patterns
            while (entry.hasNext()) { // Iterate through groups
                String key = entry.next(); // Get the current group
                System.out.printf("%s : %s%n", key, entry.group(key)); // See the group's name and its content
            }
        }
    }
    // You can use this method to easily access the functionality, without initializing the RegexHelper
    // It allows using several patterns too
    public static RegexHelper compileRegex(String... patterns) {
        return new RegexHelper(patterns);
    }

    // Main class
    public static class RegexHelper implements Iterable<Entry> {
        // Map of the patterns and the group names, related to each of the pattern
        private final Map<String, List<String>> patternsHolder = new HashMap<>();
        // For iteration
        private Entry currentEntry;
        private int currentEntryId = 0;
        // Entry has all the matches for a pattern
        private final List<Entry> matchResults = new ArrayList<>();
        // Contains log
        private final List<String> log = new ArrayList<>();
        // Empty RegexHelper
        public RegexHelper() {}
        // RegexHelper with one or several patterns
        public RegexHelper(String... patterns) {
            add(patterns);
        }
        // Add a new pattern (or several) at the flow, after initialization
        public RegexHelper add(String... patterns) {
            String regex = "\\?<(?<group>[a-zA-Z]+)>";
            Pattern p = Pattern.compile(regex);
            for (String pattern : patterns) {
                log.add(String.format("Added pattern: %s", pattern));
                Matcher m = p.matcher(pattern);
                List<String> groups = new ArrayList<>();
                while (m.find()) {
                    log.add(String.format("Added group: %s", m.group("group")));
                    groups.add(m.group("group"));
                }
                patternsHolder.put(pattern, groups);
            }
            log.add("");
            return this;
        }
        // Main method to get the match result
        public RegexHelper match(String text) {
            return match(text, "");
        }
        // Same with .find()'s step
        public RegexHelper match(String text, String iteratorRegex) {
            matchResults.clear(); // Clear the prev results on a new match
            currentEntryId = 0; // Reset the iterator

            List<Integer> endPoss = new ArrayList<>(); // For .find()'s steps
            if (!iteratorRegex.isEmpty()) { // We find all the end positions of occurrences of iteratorRegex in the text
                Pattern p = Pattern.compile(iteratorRegex);
                Matcher m = p.matcher(text);
                while (m.find()) {
                    log.add(String.format("Found an iterator \"%s\" at %d (%s).", iteratorRegex, m.end(0), m.group(0)));
                    endPoss.add(m.end(0));
                }
            }

            for (Map.Entry<String, List<String>> entry : patternsHolder.entrySet()) { // For each pattern
                int i = 0; // For steps
                Pattern p = Pattern.compile(entry.getKey());
                Matcher m = p.matcher(text);
                int start = 0; // Next step
                while (m.find(start)) {
                    Entry temp = new Entry();
                    for (String group : entry.getValue()) { // For each group
                        log.add(String.format("Found group \"%s\" is set to \"%s\" at: %s", group, m.group(group), text));
                        // If we haven't encountered a group with this name, match, and position yet...
                        Optional<Entry> match = matchResults.stream().filter(r -> r.containsValue(group, m.group(group), m.end(group))).findFirst();
                        if (match.isEmpty()) {
                            temp.put(group, m.group(group), m.end(group)); // Store the group name, its match, and end position
                        }
                    }
                    matchResults.add(temp); // Add all groups as an Entry
                    if (!endPoss.isEmpty() && i < endPoss.size() && (endPoss.get(i) != m.end(i))) { // Move "start"
                        log.add(String.format("Moved regex iterator to the %d character.", endPoss.get(i)));
                        start = endPoss.get(i++);
                    } else {
                        start = m.end(0);
                    }
                }
            }
            return this;
        }
        // Move RegexHelper's iterator to some entry
        public void setCurrentEntry(Entry entry) {
            if (!matchResults.contains(entry)) {
                System.err.printf("RegEx does not have an entry, but it is tried to force-set to it: %s%n", entry.toString());
                return;
            }
            this.currentEntry = entry;
            this.currentEntryId = matchResults.indexOf(entry);
        }
        // You can use .find() as before with original Matcher
        public boolean find() {
            if (currentEntryId >= matchResults.size()) {
                return false;
            }
            currentEntry = matchResults.get(currentEntryId++);
            return true;
        }
        // Get a group from the current entry
        public String group(String group) {
            if (currentEntry == null) {
                System.err.printf("Could not get group \"%s\", because either you didn't call find() or there are no matches.%n", group);
                return "";
            }
            if (!currentEntry.containsKey(group)) {
                System.err.printf("Regex match does not contain a group of \"%s\".%n", group);
                return "";
            }
            return currentEntry.group(group);
        }
        // Print the log into the console
        public RegexHelper printLog() {
            System.out.println("### NEW REGEX CALL ###");
            log.forEach(System.out::println);
            System.out.println();
            log.clear();
            return this;
        }
        // Retrieve log as String[]
        public String[] retrieveLog() {
            String[] temp = log.toArray(new String[0]);
            log.clear();
            return temp;
        }
        // You can always retrieve the matchResults, if you want
        public List<Entry> getMatchResults() {
            return matchResults;
        }

        @Override
        @Nonnull
        public Iterator<Entry> iterator() {
            return new RegexIterator(this);
        }
    }
    // Iterator for matches
    public static class RegexIterator implements Iterator<Entry> {
        private final RegexHelper owner;
        private int count = 0;
        public RegexIterator(RegexHelper owner) {
            this.owner = owner;
        }
        @Override
        public boolean hasNext() {
            return owner.getMatchResults().size() > count;
        }

        @Override
        public Entry next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Entry nextEntry = owner.getMatchResults().get(count++);
            owner.setCurrentEntry(nextEntry);
            return nextEntry;
        }
    }
    // Entry can be used as iterator too to iterate through groups
    public static class Entry implements Iterator<String> {
        private final Map<String, String> value;
        private final List<String> keys;
        private final List<Integer> keyIds;
        private int count = 0;
        public Entry() {
            value = new HashMap<>();
            keys = new ArrayList<>();
            keyIds = new ArrayList<>();
        }
        public void put(String k, String v, int id) { // id is used to check if the match is already counted
            if (keyIds.contains(id)) {
                return;
            }
            value.put(k, v);
            keys.add(k);
            keyIds.add(id);
        }
        public boolean containsKey(String key) { // To retrieve the group we need to check if it's present
            return value.containsKey(key);
        }
        public boolean containsValue(String k, String v, int id) { // If the Entry already contains the group
            return value.containsKey(k) && value.get(k).equals(v) && keyIds.contains(id);
        }
        public String group(String key) { // Get the group
            return value.get(key);
        }
        public Map<String, String> getValue() { // Get all matches as a Map
            return value;
        }
        @Override
        public boolean hasNext() {
            return count < keys.size();
        }
        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return keys.get(count++);
        }
        @Override
        public String toString() { // Shows all matches
            List<String> temp = new ArrayList<>();
            for (int i = 0; i < keys.size(); i++) {
                temp.add(String.format("%s = \"%s\" at %d", keys.get(i), value.get(keys.get(i)), keyIds.get(i)));
            }
            return String.join("; ", temp);
        }
    }
}
