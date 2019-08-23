package org.openlca.app.tools.mapping.generator;

import org.openlca.io.maps.FlowRef;
import org.openlca.util.Strings;

class Score {

	private double keyNameMatch; // without stopwords
	private double rawNameMatch; // with all words
	private double categoryMatch;
	private double locationMatch;
	private boolean sameType;
	private boolean sameUnit;

	private Score() {
	}

	static Score compute(FlowRef sflow, FlowRef tflow, WordMatcher words) {
		Score score = new Score();
		if (sflow == null || sflow.flow == null
				|| tflow == null || tflow.flow == null)
			return score;

		score.rawNameMatch = words.matchAll(
				sflow.flow.name, tflow.flow.name);
		if (score.rawNameMatch == 0) {
			score.keyNameMatch = 0;
		} else {
			score.keyNameMatch = words.matchKeys(
					sflow.flow.name, tflow.flow.name);
		}

		score.categoryMatch = words.matchAll(
				sflow.flowCategory, tflow.flowCategory);
		score.locationMatch = words.matchAll(
				sflow.flowLocation, tflow.flowLocation);
		score.sameType = sflow.flow.type == tflow.flow.type;
		if (sflow.unit != null && tflow.unit != null
				&& Strings.nullOrEqual(sflow.unit.name, tflow.unit.name)) {
			score.sameUnit = true;
		} else {
			score.sameUnit = false;
		}
		return score;
	}

	boolean betterThan(Score other) {
		if (other == null)
			return true;
		if (this.rawNameMatch == 0)
			return false;
		if (other.rawNameMatch == 0)
			return true;

		if (other.sameType && !this.sameType)
			return false;
		if (!other.sameType && this.sameType)
			return true;

		double nameDiff = this.keyNameMatch - other.keyNameMatch;
		if (nameDiff > 0.3)
			return true;
		if (nameDiff < 0.3)
			return false;
		return this.total() > other.total();

	}

	private double total() {
		double s = rawNameMatch
				+ (0.25 * categoryMatch)
				+ (0.25 * locationMatch);
		if (sameType) {
			s *= 1.1;
		}
		if (sameUnit) {
			s *= 1.1;
		}
		return s;
	}

}
