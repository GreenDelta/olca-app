package org.openlca.app.tools.migration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.openlca.commons.Res;
import org.openlca.io.olca.migration.MatchingStrategy;
import org.openlca.io.olca.migration.MigrationPlan;
import org.openlca.io.olca.migration.ProviderInfo;
import org.openlca.io.olca.migration.ProviderMatch;
import org.openlca.jsonld.Json;

import com.google.gson.JsonObject;

class MatchDumpLoader {

	private final MigrationPlan plan;
	private final File file;

	private MatchDumpLoader(MigrationPlan plan, File file) {
		this.plan = plan;
		this.file = file;
	}

	static Res<MatchDumpStats> apply(MigrationPlan plan, File file) {
		return plan != null && file != null
			? new MatchDumpLoader(plan, file).apply()
			: Res.error("Plan or file is null");
	}

	private Res<MatchDumpStats> apply() {

		var res = DumpIndex.readFrom(file);
		if (res.isError())
			return res.wrapError("Failed to read file");
		var index = res.value();

		int found = 0;
		int updated = 0;
		for (var match : plan.providerMatches()) {
			var entry = index.find(match);
			if (entry == null)
				continue;
			found++;
			if (entry.checkUpdate(match)) {
				updated++;
			}
		}

		var stats = new MatchDumpStats(
			plan.providerMatches().size(),
			index.data.size(),
			found,
			updated
		);
		return Res.ok(stats);
	}

	private record DumpIndex(Map<String, Entry> data) {

		static Res<DumpIndex> readFrom(File file) {
			if (file == null || !file.isFile())
				return Res.error("No file provided");
			try {
				var root = Json.readObject(file).orElse(null);
				if (root == null)
					return Res.error("Failed to read dump from file: " + file);
				var matches = Json.getArray(root, "matches");
				if (matches == null) {
					return Res.error(
						"The provided file does not contain any provider matches: " + file);
				}

				var index = new DumpIndex(new HashMap<>(matches.size()));
				for (var e : matches) {
					if (!e.isJsonObject())
						continue;
					var entry = Entry.of(e.getAsJsonObject());
					index.put(entry);
				}
				return Res.ok(index);
			} catch (Exception e) {
				return Res.error("Failed to read dump file: " + file, e);
			}
		}

		void put(Entry entry) {
			var key = entry.provider + " :: " + entry.flow;
			data.put(key, entry);
		}

		@Nullable
		Entry find(ProviderMatch match) {
			if (match == null || !match.isComplete())
				return null;
			var key = match.source().provider().refId
				+ " :: " + match.source().flow().refId;
			return data.get(key);
		}

	}

	private record Entry(
		String provider,
		String flow,
		String selected,
		MatchingStrategy strategy
	) {

		static Entry of(JsonObject obj) {
			return new Entry(
				Json.getString(obj, "provider"),
				Json.getString(obj, "flow"),
				Json.getString(obj, "selected"),
				Json.getEnum(obj, "strategy", MatchingStrategy.class)
			);
		}

		/// Returns `true`, if the provided match was updated with the information
		/// from this entry. This is the case, when the given match is complete but
		/// has a different selected target provider than described by this entry.
		/// In this case, it searches for the corresponding target provider
		/// described by this entry. If it finds such a provider, it updates the
		/// match and returns `true`. In all other cases, it returns `false`.
		boolean checkUpdate(ProviderMatch match) {
			if (match == null
				|| !match.isComplete()
				|| Objects.equals(this.selected, match.selected().provider().refId))
				return false;

			ProviderInfo alternative = null;
			for (var alt : match.alternatives()) {
				if (alt.provider() != null
					&& Objects.equals(selected, alt.provider().refId)) {
					alternative = alt;
					break;
				}
			}
			if (alternative == null)
				return false;

			match.select(alternative, strategy);
			return true;
		}
	}
}
