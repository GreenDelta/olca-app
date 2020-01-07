package org.openlca.app.tools.mapping.generator;

import org.openlca.core.model.FlowType;
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

		String scategory = sflow.flowCategory;
		if (sflow.flow.flowType == FlowType.ELEMENTARY_FLOW) {
			scategory = stemCompartment(scategory);
		}
		String tcategory = tflow.flowCategory;
		if (tflow.flow.flowType == FlowType.ELEMENTARY_FLOW) {
			tcategory = stemCompartment(tcategory);
		}
		score.categoryMatch = words.matchAll(
				scategory, tcategory);

		score.locationMatch = words.matchAll(
				sflow.flowLocation, tflow.flowLocation);
		score.sameType = sflow.flow.flowType == tflow.flow.flowType;
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
		if (nameDiff > 0.1)
			return true;
		else
			return this.total() > other.total();
	}

	private double total() {
		double s = rawNameMatch
				+ (0.2 * categoryMatch)
				+ (0.1 * locationMatch);
		if (sameType) {
			s *= 1.1;
		}
		if (sameUnit) {
			s *= 1.1;
		}
		return s;
	}

	private static String stemCompartment(String s) {
		if (Strings.nullOrEmpty(s))
			return "";
		String[] parts = s.toLowerCase().split("/");
		String path = "";
		String[] stopwords = {
				"elementary",
				"flows",
				"unspecified",
				"emission",
				"emissions",
				"to",
				"in",
				"from",
				"and"
		};
		for (String part : parts) {
			String p = part.trim();
			if (p.isEmpty())
				continue;
			String[] words = p.split(" ");
			p = "";
			for (String word : words) {
				for (String stop : stopwords) {
					if (stop.equals(word))
						continue;
				}
				if (path.contains(word))
					continue;
				p = p.length() == 0 ? word : p + " " + word;
			}
			path = path.length() == 0 ? p : path + "/" + p;
		}
		return path;
	}

}
