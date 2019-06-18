package org.openlca.app.tools.mapping.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openlca.io.maps.FlowRef;
import org.openlca.util.Strings;

class Matcher {

	private final Map<String, FlowRef> targetFlows;

	Matcher(List<FlowRef> targetFlows) {
		this.targetFlows = targetFlows.stream()
				.filter(f -> f.flow != null && f.flow.refId != null)
				.collect(Collectors.toMap(f -> f.flow.refId, f -> f));
	}

	FlowRef find(FlowRef sflow) {
		if (sflow == null || sflow.flow == null
				|| sflow.flow.refId == null)
			return null;

		// test whether there is a direct match based
		// on the reference IDs
		FlowRef tflow = targetFlows.get(sflow.flow.refId);
		if (tflow != null)
			return tflow;

		FlowRef candidate = null;
		double score = 0.0;
		for (FlowRef c : targetFlows.values()) {
			double s = score(sflow, c);
			if (s > score) {
				candidate = c;
				score = s;
			}
		}
		return candidate;
	}

	private double score(FlowRef sflow, FlowRef tflow) {
		if (sflow.flow == null || tflow.flow == null)
			return 0;
		double nameScore = score(sflow.flow.name, tflow.flow.name);
		double catScore = score(sflow.flowCategory, tflow.flowCategory);
		double locScore = score(sflow.flowLocation, tflow.flowLocation);

		double score = nameScore + (0.25 * catScore) + (0.25 * locScore);
		if (sflow.flow.flowType == tflow.flow.flowType) {
			score *= 1.1;
		}
		// TODO flow property, unit, location in names etc.
		return score;
	}

	private double score(String a, String b) {
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

	private List<String> keywords(String s) {
		if (Strings.nullOrEmpty(s))
			return Collections.emptyList();

		StringBuilder buf = new StringBuilder();
		List<String> words = new ArrayList<>();
		for (char c : s.trim().toLowerCase().toCharArray()) {
			if (Character.isWhitespace(c)
					|| c == ',' || c == ';' || c == '/' || c == '-') {
				if (buf.length() > 0) {
					words.add(buf.toString());
					buf = new StringBuilder();
				}
				continue;
			}
			buf.append(c);
		}
		if (buf.length() > 0) {
			words.add(buf.toString());
		}
		return words;
	}
}
