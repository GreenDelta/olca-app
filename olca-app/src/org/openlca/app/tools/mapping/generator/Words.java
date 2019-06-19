package org.openlca.app.tools.mapping.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openlca.util.Strings;

final class Words {

	private Words() {
	}

	/**
	 * Returns a score that indicates how well the (key) words in string A match
	 * the (key) words in string B. It returns a value between `0` and `1` where
	 * `0` means `no match` and `1` means `complete match`. Non-alphanumeric are
	 * used as word separators.
	 */
	static double match(String a, String b) {
		if (a == null || b == null)
			return 0.0;

		String[] wordsA = keywords(a);
		String[] wordsB = keywords(b);
		if (wordsA.length == 0 || wordsB.length == 0)
			return 0;

		double total = 0;
		double matched = 0;
		for (int i = 0; i < wordsA.length; i++) {
			String wordA = wordsA[i];

			double max = 5 * 1.2 * wordA.length();
			total += max;
			double matchedPart = 0;

			for (int j = 0; j < wordsB.length; j++) {
				double distFactor = 1.2 - Math.abs(i - j) / 20;
				String wordB = wordsB[j];

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

	private static String[] keywords(String s) {
		if (Strings.nullOrEmpty(s))
			return new String[0];

		StringBuilder buf = new StringBuilder();
		List<String> words = new ArrayList<>();
		for (char c : s.toLowerCase().toCharArray()) {
			if (Character.isLetterOrDigit(c)) {
				buf.append(c);
			} else if (buf.length() > 0) {
				words.add(buf.toString());
				buf = new StringBuilder();
			}
		}
		if (buf.length() > 0) {
			words.add(buf.toString());
		}
		return words.toArray(new String[words.size()]);
	}
}
