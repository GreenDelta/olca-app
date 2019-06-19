package org.openlca.app.tools.mapping.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.model.ILCDProvider;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.io.maps.FlowRef;
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
		List<String> wordsA = keywords(a);
		List<String> wordsB = keywords(b);
		if (wordsA.isEmpty() || wordsB.isEmpty())
			return 0;

		// make sure that the size of A is larger
		if (wordsB.size() > wordsA.size()) {
			List<String> tmp = wordsA;
			wordsA = wordsB;
			wordsB = tmp;
		}

		double totalLen = 0;
		double matchedLen = 0;
		for (String wordA : wordsA) {
			totalLen += wordA.length();
			if (wordsB.contains(wordA)) {
				matchedLen += wordA.length();
				wordsB.remove(wordA);
			}
		}

		if (totalLen == 0)
			return 0;
		return matchedLen / totalLen;
	}

	private static List<String> keywords(String s) {
		if (Strings.nullOrEmpty(s))
			return Collections.emptyList();

		StringBuilder buf = new StringBuilder();
		List<String> words = new ArrayList<>();
		for (char c : s.trim().toLowerCase().toCharArray()) {
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
		return words;
	}

	// TODO just for testing
	public static void main(String[] args) {

		String dbDir = "C:/Users/Besitzer/openLCA-data-1.4/databases/e_3_3_er_database_es2050_v1_7_1";
		IDatabase db = new DerbyDatabase(new File(dbDir));
		DBProvider target = new DBProvider(db);
		List<FlowRef> targetFlows = target.getFlowRefs();

		String ilcdPack = "C:/Users/Besitzer/Projects/_current/dlr_mapping_tool/Mahlen Baotou 2018_07_04.zip";
		ILCDProvider source = ILCDProvider.of(ilcdPack);
		List<FlowRef> sourceFlows = source.getFlowRefs();

		for (FlowRef s : sourceFlows) {
			System.out.println(s.flow.name);
			String best = "-";
			double score = 0;
			for (FlowRef t : targetFlows) {
				double sv = match(s.flow.name, t.flow.name);
				if (sv > 0) {
					System.out.println("    ? " + t.flow.name + "  ;; score = " + sv);
				}
				if (sv > score) {
					best = t.flow.name;
					score = sv;
				}
			}
			System.out.println("  -> " + best + " ;; score = " + score);
		}

		System.out.println(keywords("transport, freight, lorry >32 metric ton, EURO3"));

	}

}
