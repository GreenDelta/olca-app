package org.openlca.app.editors;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Table;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

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
		String[] existing = of(model);
		if (Strings.nullOrEmpty(tag))
			return existing;
		String newTag = tag.trim();
		for (String ex : existing) {
			if (newTag.equalsIgnoreCase(ex))
				return existing;
		}
		String[] next = Arrays.copyOf(existing, existing.length + 1);
		next[existing.length] = newTag;
		model.tags = String.join(",", next);
		return next;
	}

	public static String[] remove(CategorizedEntity model, String tag) {
		String[] existing = of(model);
		if (Strings.nullOrEmpty(tag))
			return existing;
		String[] next = Arrays.stream(existing)
				.filter(other -> !tag.equalsIgnoreCase(other))
				.toArray(String[]::new);
		model.tags = String.join(",", next);
		return next;
	}

	/**
	 * Search for possible tags of the given model.
	 */
	public static String[] searchFor(CategorizedEntity model, IDatabase db) {
		if (model == null)
			return new String[0];
		Class<?> type = model.getClass();
		if (!type.isAnnotationPresent(Table.class))
			return new String[0];

		// collect candidates from the model table
		Table table = type.getAnnotation(Table.class);
		String sql = "select tags from " + table.name();
		Set<String> candidates = new HashSet<>();
		try {
			NativeSql.on(db).query(sql, r -> {
				String tags = r.getString(1);
				if (Strings.nullOrEmpty(tags))
					return true;
				Arrays.stream(tags.split(","))
						.filter(tag -> !Strings.nullOrEmpty(tag))
						.map(String::trim)
						.forEach(candidates::add);
				return true;
			});
		} catch (SQLException e) {
			LoggerFactory.getLogger(Tags.class).error("Error searching for tags", e);
			return new String[0];
		}

		// remove existing tags from candidates
		for (String existing : of(model)) {
			candidates.remove(existing);
		}
		return candidates.stream()
				.sorted()
				.toArray(String[]::new);
	}
}
