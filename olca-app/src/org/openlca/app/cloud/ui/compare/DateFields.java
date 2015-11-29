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

	static {
		for (ModelType type : ModelType.values())
			if (type != ModelType.UNKNOWN)
				put(type.getModelClass(), "lastChange");
		put(ProcessDocumentation.class, "validFrom");
		put(ProcessDocumentation.class, "validUntil");
		put(ProcessDocumentation.class, "creationDate");
		put(Project.class, "creationDate");
		put(Project.class, "lastModificationDate");		
	}

	private static void put(Class<?> clazz, String property) {
		if (!timestampFields.containsKey(clazz.getSimpleName()))
			timestampFields.put(clazz.getSimpleName(), new HashSet<>());
		timestampFields.get(clazz.getSimpleName()).add(property);
	}

	static boolean isTimestamp(JsonElement element, String property) {
		String type = ModelUtil.getType(element);
		if (type == null)
			return false;
		if (!timestampFields.containsKey(type))
			return false;
		return timestampFields.get(type).contains(property);
	}

}
