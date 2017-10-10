package org.openlca.app.cloud.ui.compare;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.Project;

import com.google.gson.JsonElement;

class DateFields {

	private static Map<String, Set<String>> timestampFields = new HashMap<>();
	private static Map<String, Set<String>> dateFields = new HashMap<>();

	static {
		for (ModelType type : ModelType.values())
			if (type != ModelType.UNKNOWN)
				putTimestamp(type.getModelClass(), "lastChange");
		putDate(ProcessDocumentation.class, "validFrom");
		putDate(ProcessDocumentation.class, "validUntil");
		putTimestamp(ProcessDocumentation.class, "creationDate");
		putTimestamp(Project.class, "creationDate");
		putTimestamp(Project.class, "lastModificationDate");
	}

	private static void putTimestamp(Class<?> clazz, String property) {
		if (!timestampFields.containsKey(clazz.getSimpleName()))
			timestampFields.put(clazz.getSimpleName(), new HashSet<>());
		timestampFields.get(clazz.getSimpleName()).add(property);
	}

	private static void putDate(Class<?> clazz, String property) {
		if (!dateFields.containsKey(clazz.getSimpleName()))
			dateFields.put(clazz.getSimpleName(), new HashSet<>());
		dateFields.get(clazz.getSimpleName()).add(property);
	}

	static boolean isTimestamp(JsonElement element, String property) {
		String type = ModelUtil.getType(element);
		if (type == null)
			return false;
		if (!timestampFields.containsKey(type))
			return false;
		return timestampFields.get(type).contains(property);
	}

	static boolean isDate(JsonElement element, String property) {
		String type = ModelUtil.getType(element);
		if (type == null)
			return false;
		if (!dateFields.containsKey(type))
			return false;
		return dateFields.get(type).contains(property);
	}

	static boolean isDateOrTimestamp(JsonElement element, String property) {
		return isDate(element, property) || isTimestamp(element, property);
	}

}
