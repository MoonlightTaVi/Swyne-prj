package utils;

import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiBot {
	public static void main(String[] args) {
		MediaWikiBot wikiBot = new MediaWikiBot("https://ru.wiktionary.org/w/api.php");
		Article article = wikiBot.getArticle("дубина");
		//System.out.println(getSynonyms(article.getText()));
		System.out.println(article.getText());
	}
	// Find all synonyms in a given Wiktionary article
	public static List<String> getSynonyms(String articleText) {
		List<String> ret = new ArrayList<>();
		String regexMain = "==== Синонимы ====(.*?)(\\n\\s*\\n|$)"; // First, find the section
		Pattern patternMain = Pattern.compile(regexMain, Pattern.DOTALL); // DOTALL allows multiline capturing
		Matcher matcherMain = patternMain.matcher(articleText);
		while (matcherMain.find()) { // If there is a single match
			String regexWord = "\\[\\[(.*?)]]"; // We extract each synonymous word
			Pattern patternWord = Pattern.compile(regexWord); // We DO NOT need DOTALL here!
			Matcher matcherWord = patternWord.matcher(matcherMain.group(1));
			while (matcherWord.find()) { // For each word (match)
				//System.out.println(matcherWord.group(1));
				ret.add(matcherWord.group(1)); // We fill in the on-return list
			}
		}
		return ret;
	}
}
