package regex;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {
    public static void main(String[] args) {
        RegexHelper regex = new RegexHelper("(?<fruit>(яблоко|апельсин|персик))",
                "(?<several>(?<firstWord>[а-яА-Я]+)(, ([а-яА-Я]+))*)").match("яблоко, книга, апельсин");
        for (Entry entry : regex) {
            while (entry.hasNext()) {
                String key = entry.next();
                System.out.printf("%s : %s%n", key, entry.group(key));
            }
        }
    }
    public static RegexHelper compileRegex(String... patterns) {
        return new RegexHelper(patterns);
    }

    public static class RegexHelper implements Iterable<Entry> {
        private final Map<String, Set<String>> patternsHolder = new HashMap<>();
        private Entry currentEntry;
        private int currentEntryId = 0;
        private final List<Entry> matchResults = new ArrayList<>();
        private final List<String> log = new ArrayList<>();
        public RegexHelper() {}
        public RegexHelper(String... patterns) {
            add(patterns);
        }
        public RegexHelper add(String... patterns) {
            String regex = "\\?<(?<group>[a-zA-Z]+)>";
            Pattern p = Pattern.compile(regex);
            for (String pattern : patterns) {
                log.add(String.format("Added pattern: %s", pattern));
                Matcher m = p.matcher(pattern);
                Set<String> groups = new HashSet<>();
                while (m.find()) {
                    log.add(String.format("Added group: %s", m.group("group")));
                    groups.add(m.group("group"));
                }
                patternsHolder.put(pattern, groups);
            }
            return this;
        }
        public RegexHelper match(String text) {
            matchResults.clear();
            currentEntryId = 0;
            for (Map.Entry<String, Set<String>> entry : patternsHolder.entrySet()) {
                Pattern p = Pattern.compile(entry.getKey());
                Matcher m = p.matcher(text);
                while (m.find()) {
                    Entry temp = new Entry();
                    for (String group : entry.getValue()) {
                        log.add(String.format("Found group \"%s\" is set to \"%s\" at: %s", group, m.group(group), m.group()));
                        temp.put(group, m.group(group));
                    }
                    matchResults.add(temp);
                }
            }
            return this;
        }
        public RegexHelper match(String text, boolean doLogging) {
            match(text);
            if (doLogging) {
                printLog();
            }
            return this;
        }
        public void setCurrentEntry(Entry entry) {
            if (!matchResults.contains(entry)) {
                System.err.printf("RegEx does not have an entry, but it is tried to force-set to it: %s%n", entry.toString());
                return;
            }
            this.currentEntry = entry;
            this.currentEntryId = matchResults.indexOf(entry);
        }
        public boolean find() {
            if (currentEntryId >= matchResults.size()) {
                return false;
            }
            currentEntry = matchResults.get(currentEntryId++);
            return true;
        }
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
        public void printLog() {
            log.forEach(System.out::println);
            log.clear();
        }
        public List<Entry> getMatchResults() {
            return matchResults;
        }

        @Override
        @Nonnull
        public Iterator<Entry> iterator() {
            return new RegexIterator(this);
        }
    }
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
    public static class Entry implements Iterator<String> {
        private final Map<String, String> value;
        private final List<String> keys;
        private int count = 0;
        public Entry() {
            value = new HashMap<>();
            keys = new ArrayList<>();
        }
        public Entry(Map<String, String> map) {
            value = map;
            keys = value.keySet().stream().toList();
        }
        public void put(String k, String v) {
            value.put(k, v);
            keys.add(k);
        }
        public boolean containsKey(String key) {
            return value.containsKey(key);
        }
        public String group(String key) {
            return value.get(key);
        }
        public Map<String, String> getValue() {
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
        public String toString() {
            return value.toString();
        }
    }
}
