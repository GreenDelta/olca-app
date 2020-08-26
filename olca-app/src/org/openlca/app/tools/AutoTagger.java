package org.openlca.app.tools;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.CategoryPathBuilder;
import org.openlca.util.Pair;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * Tries to tag processes in the database automatically.
 */
public class AutoTagger implements Runnable {

	final int minWordLen = 5;
	final int maxTagCount = 8;
	private final IDatabase db;

	public AutoTagger(IDatabase db) {
		this.db = db;
	}

	@Override
	public void run() {

		// compute statistics
		Map<String, Integer> stats = new HashMap<>();
		CategoryPathBuilder paths = new CategoryPathBuilder(db);
		TLongObjectHashMap<ProcessDescriptor> descriptors = new ProcessDao(db).descriptorMap();
		descriptors.forEachValue(d -> {
			addToStats(d.name, stats);
			if (d.category != null) {
				addToStats(paths.build(d.category), stats);
			}
			return true;
		});

		// add auto-tags
		try {
			String sql = "select id, tags from tbl_processes";
			NativeSql.on(db).updateRows(sql, r -> {
				ProcessDescriptor d = descriptors.get(r.getLong(1));
				if (d == null)
					return true;

				// do nothing if there are already tags present
				String tags = r.getString(2);
				if (!Strings.nullOrEmpty(tags))
					return true;

				Set<String> candidates = new HashSet<>(words(d.name));
				if (d.category != null) {
					candidates.addAll(words(paths.build(d.category)));
				}

				String[] tagList = candidates.stream()
						.map(word -> Pair.of(word, stats.getOrDefault(word, 0)))
						.filter(pair -> pair.second > 0)
						.sorted((pair1, pair2) -> pair2.second - pair1.second)
						.limit(maxTagCount)
						.map(pair -> pair.first)
						.toArray(String[]::new);
				if (tagList.length == 0)
					return true;
				tags = String.join(",", tagList);
				r.updateString(2, tags);
				r.updateRow();
				return true;
			});
		} catch (SQLException e) {
			LoggerFactory.getLogger(AutoTagger.class).error("Error during auto tagging", e);
		}

		db.clearCache();
	}

	private void addToStats(String phrase, Map<String, Integer> stats) {
		for (String word : words(phrase)) {
			stats.compute(word,
					(_w, count) -> count == null ? 1 : count + 1);
		}
	}

	private List<String> words(String phrase) {
		if (Strings.nullOrEmpty(phrase))
			return Collections.emptyList();
		List<String> words = new ArrayList<>();
		StringBuilder word = new StringBuilder();
		for (char c : phrase.toCharArray()) {
			if (Character.isLetter(c)) {
				word.append(c);
				continue;
			}
			if ((Character.isDigit(c) || c == '-')
					&& word.length() > 0) {
				word.append(c);
				continue;
			}
			if (word.length() > 0) {
				String tag = word.toString().toLowerCase();
				if (tag.length() >= minWordLen) {
					words.add(tag);
				}
				word.setLength(0);
			}
		}
		return words;
	}
}
