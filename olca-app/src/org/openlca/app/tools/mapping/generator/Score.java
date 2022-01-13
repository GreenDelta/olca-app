package org.openlca.app.tools.mapping.generator;

import org.openlca.core.model.FlowType;
import org.openlca.io.maps.FlowRef;
import org.openlca.util.Strings;

record Score(
	double nameScore,
	double categoryScore,
	double locationScore,
	boolean sameType,
	boolean sameUnit) {

	private static final Score noMatch = new Score(0, 0, 0, false, false);

	static Score noMatch() {
		return noMatch;
	}

	boolean isNoMatch() {
		return this == noMatch || nameScore == 0;
	}

	static Score compute(Matcher matcher, FlowRef s, FlowRef t) {
		if (s == null
			|| s.flow == null
			|| t == null
			|| t.flow == null)
			return noMatch;

		double nameScore = s.flow.name != null && t.flow.name != null
			? matcher.similarityOf(s.flow.name, t.flow.name)
			: 0;
		if (nameScore == 0)
			return noMatch;

		double categoryScore = ofCategories(matcher, s, t);
		double locationScore = s.flowLocation != null && t.flowLocation != null
			? matcher.similarityOf(s.flowLocation, t.flowLocation)
			: 0;

		boolean sameType = s.flow.flowType == t.flow.flowType;
		boolean sameUnit = s.unit != null
			&& t.unit != null
			&& Strings.nullOrEqual(s.unit.name, t.unit.name);

		return new Score(nameScore, categoryScore, locationScore, sameType, sameUnit);
	}

	private static double ofCategories(Matcher matcher, FlowRef s, FlowRef t) {
		if (s.flow.flowType != FlowType.ELEMENTARY_FLOW
			|| t.flow.flowType != FlowType.ELEMENTARY_FLOW
			|| s.flowCategory == null
			|| t.flowCategory == null)
			return 0;
		var stemmed1 = matcher.compartmentStemmer.stem(s.flowCategory);
		var stemmed2 = matcher.compartmentStemmer.stem(t.flowCategory);
		int minLen = Math.min(stemmed1.length, stemmed2.length);
		if (minLen == 0)
			return 0;
		double maxLen = Math.max(stemmed1.length, stemmed2.length);
		double overlap = 0;
		double pathFactor = 1;
		for (int i = 0; i < minLen; i++) {
			var s1 = stemmed1[i];
			var s2 = stemmed2[i];
			if (s1.equals(s2)) {
				overlap += pathFactor;
				continue;
			}
			double sim = matcher.similarityOf(s1, s2);
			if (sim == 0)
				break;
			pathFactor *= sim;
			overlap += pathFactor;
		}
		return overlap / maxLen;
	}

	boolean betterThan(Score other) {
		if (this.isNoMatch())
			return false;
		if (other.isNoMatch())
			return true;

		double diff = this.nameScore - other.nameScore;

		// raw filter
		if (diff > 0.75)
			return true;
		if (diff < -0.75)
			return false;

		if (this.sameType != other.sameType)
			return this.sameType;

		if (this.categoryScore > 0 && other.categoryScore == 0)
			return true;
		if (other.categoryScore > 0 && this.categoryScore == 0)
			return false;

		return this.total() > other.total();
	}

	private double total() {
		double s = nameScore
			+ (0.3 * categoryScore)
			+ (0.1 * locationScore);
		if (sameType) {
			s *= 1.1;
		}
		if (sameUnit) {
			s *= 1.1;
		}
		return s;
	}
}
