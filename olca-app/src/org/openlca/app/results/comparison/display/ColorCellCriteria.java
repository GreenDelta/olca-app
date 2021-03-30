package org.openlca.app.results.comparison.display;

public enum ColorCellCriteria {
	NONE(""), CATEGORY("Category"), LOCATION("Location");

	private String criteria;

	ColorCellCriteria(String c) {
		criteria = c;
	}

	public static ColorCellCriteria getCriteria(String c) {
		for (ColorCellCriteria comparisonCriteria : values()) {
			if (comparisonCriteria.criteria.equals(c)) {
				return comparisonCriteria;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return criteria;
	}

	public static String[] valuesToString() {
		var criterias = values();
		String[] crits = new String[criterias.length];
		for (int i = 0; i < criterias.length; i++) {
			crits[i] = criterias[i].toString();
		}
		return crits;
	}
}
