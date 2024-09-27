package swyne.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import regex.RegexUtils.*;
import static regex.RegexUtils.compileRegex;

public class FileReader {
    private String directory = "";
    private List<String> paths = new ArrayList<>();
    public FileReader setDirectory(String path) {
        if (path.charAt(path.length()-1) != '/') {
            path += "/";
        }
        directory = path;
        return this;
    }
    public FileReader add(String path) {
        paths.add(directory + path);
        return this;
    }
    public String readAllToString() {
        StringBuilder ret = new StringBuilder();
        for (String path : paths) {
            try (Scanner s = new Scanner(new File(path))) {
                while (s.hasNext()) {
                    String line = s.nextLine();
                    line = line.replaceAll("/\\*(?:[^*]|\\*+[^*/])*\\*+/|//.*","");
                    line = line.replaceAll("\\s\\s+"," ");
                    ret.append(line);
                    ret.append(" ");
                }
            } catch (FileNotFoundException e) {
                System.err.printf("File not found: %s%n", path);
            }
        }
        return ret.toString();
    }
}
