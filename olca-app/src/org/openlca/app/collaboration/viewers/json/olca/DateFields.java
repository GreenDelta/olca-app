package org.openlca.app.collaboration.viewers.json.olca;

import java.util.HashSet;
import java.util.Set;

class DateFields {

	private static Set<String> timestampFields = new HashSet<>();
	private static Set<String> dateFields = new HashSet<>();

	static {
		timestampFields.add("lastChange");
		timestampFields.add("creationDate");
		dateFields.add("validFrom");
		dateFields.add("validUntil");
	}

	static boolean isTimestamp(String property) {
		return timestampFields.contains(property);
	}

	static boolean isDate(String property) {
		return dateFields.contains(property);
	}

	static boolean isDateOrTimestamp(String property) {
		return isDate(property) || isTimestamp(property);
	}

}
