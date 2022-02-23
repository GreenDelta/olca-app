package org.openlca.app.editors;

import java.util.Arrays;
import java.util.HashSet;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.RootEntity;
import org.openlca.util.Strings;

import jakarta.persistence.Table;

final class Tags {

	private Tags() {
	}

	public static String[] of(RootEntity model) {
		if (model == null || Strings.nullOrEmpty(model.tags))
			return new String[0];
		return Arrays.stream(model.tags.split(","))
				.filter(tag -> !Strings.nullOrEmpty(tag))
				.map(String::trim)
				.toArray(String[]::new);
	}

	public static String[] add(RootEntity model, String tag) {
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

	public static String[] remove(RootEntity model, String tag) {
		var existing = of(model);
		if (Strings.nullOrEmpty(tag))
			return existing;
		var next = Arrays.stream(existing)
				.filter(other -> !tag.equalsIgnoreCase(other))
				.toArray(String[]::new);
		model.tags = String.join(",", next);
		return next;
	}

	/**
	 * Search for possible tags of the given model.
	 */
	public static String[] searchFor(RootEntity model, IDatabase db) {
		if (model == null)
			return new String[0];
		var type = model.getClass();
		if (!type.isAnnotationPresent(Table.class))
			return new String[0];

		// collect candidates from the model table
		var table = type.getAnnotation(Table.class);
		var sql = "select tags from " + table.name();
		var candidates = new HashSet<String>();
		NativeSql.on(db).query(sql, r -> {
			var tags = r.getString(1);
			if (Strings.nullOrEmpty(tags))
				return true;
			Arrays.stream(tags.split(","))
					.filter(tag -> !Strings.nullOrEmpty(tag))
					.map(String::trim)
					.forEach(candidates::add);
			return true;
		});

		// remove existing tags from candidates
		for (var existing : of(model)) {
			candidates.remove(existing);
		}
		return candidates.stream()
				.sorted()
				.toArray(String[]::new);
	}
}
