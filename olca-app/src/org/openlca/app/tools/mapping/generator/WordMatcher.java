package org.openlca.app.tools.mapping.generator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WordMatcher {

	private final Set<String> stopwords = new HashSet<>();

	public WordMatcher() {
		try (InputStream is = getClass().getResourceAsStream("stopwords.txt");
				InputStreamReader reader = new InputStreamReader(is, "utf-8");
				BufferedReader buf = new BufferedReader(reader)) {
			String line;
			while ((line = buf.readLine()) != null) {
				String s = line.trim().toLowerCase();
				if (s.isEmpty())
					continue;
				stopwords.add(s);
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to read stopwords", e);
		}
	}

	/**
	 * Returns a score that indicates how well the words in string A match the
	 * words in string B. It returns a value between `0` and `1` where `0` means
	 * `no match` and `1` means `complete match`. Non-alphanumeric are used as
	 * word separators.
	 */
	double matchAll(String a, String b) {
		return match(a, b, false);
	}

	/**
	 * Same as `matchAllWords` but stopwords are ignored when calculating the
	 * score.
	 */
	double matchKeys(String a, String b) {
		return match(a, b, true);
	}

	private double match(String a, String b, boolean withoutStopwords) {
		if (a == null || b == null)
			return 0.0;

		List<String> wordsA = words(a, withoutStopwords);
		List<String> wordsB = words(b, withoutStopwords);
		if (wordsA.size() == 0 || wordsB.size() == 0)
			return 0;

		double total = 0;
		double matched = 0;
		for (int i = 0; i < wordsA.size(); i++) {
			String wordA = wordsA.get(i);

			double max = 5 * 1.2 * wordA.length();
			total += max;
			double matchedPart = 0;

			for (int j = 0; j < wordsB.size(); j++) {
				double distFactor = 1.2 - Math.abs(i - j) / 20;
				String wordB = wordsB.get(j);

				if (Objects.equals(wordA, wordB)) {
					double m = 5 * distFactor * wordB.length();
					if (m > matchedPart) {
						matchedPart = m;
					}
					continue;
				}

				String wa = wordA;
				String wb = wordB;
				if (wa.length() > wb.length()) {
					wa = wordB;
					wb = wordA;
				}

				if (wb.contains(wa)) {
					double m = distFactor * ((double) wa.length())
							/ ((double) wb.length());
					if (m > matchedPart) {
						matchedPart = m;
					}
				}
			}
			matched += matchedPart;
		}

		if (total == 0)
			return 0;
		return matched / total;
	}

	/**
	 * Extracts the single words (in lower case) from the given string.
	 */
	private List<String> words(String s, boolean withoutStopwords) {
		if (Strings.nullOrEmpty(s))
			return Collections.emptyList();

		StringBuilder buf = new StringBuilder();
		List<String> words = new ArrayList<>();
		for (char c : s.toLowerCase().toCharArray()) {
			if (Character.isLetterOrDigit(c)) {
				buf.append(c);
			} else if (buf.length() > 0) {
				String word = buf.toString();
				buf = new StringBuilder();
				boolean isKeyWord = !stopwords.contains(word);
				if (isKeyWord || !withoutStopwords) {
					words.add(word);
				}
			}
		}
		if (buf.length() > 0) {
			String word = buf.toString();
			boolean isKeyWord = !stopwords.contains(word);
			if (isKeyWord || !withoutStopwords) {
				words.add(word);
			}
		}
		return words;
	}
}
