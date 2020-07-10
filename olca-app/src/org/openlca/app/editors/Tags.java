package org.openlca.app.editors;

import java.util.Arrays;

import org.openlca.core.model.CategorizedEntity;
import org.openlca.util.Strings;

final class Tags {

	private Tags() {
	}

	public static String[] of(CategorizedEntity model) {
		if (model == null || Strings.nullOrEmpty(model.tags))
			return new String[0];
		return Arrays.stream(model.tags.split(","))
				.filter(tag -> !Strings.nullOrEmpty(tag))
				.map(String::trim)
				.toArray(String[]::new);
	}

	public static String[] add(CategorizedEntity model, String tag) {
		var existing = of(model);
		if (Strings.nullOrEmpty(tag))
			return existing;
		var newTag = tag.trim();
		for (var ex : existing) {
			if (newTag.equalsIgnoreCase(ex))
				return existing;
		}
		var next = Arrays.copyOf(existing, existing.length + 1);
		next[existing.length] = newTag;
		model.tags = String.join(",", next);
		return next;
	}
}
