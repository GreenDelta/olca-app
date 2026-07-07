package org.openlca.app.tools.migration;

import static org.openlca.jsonld.Json.*;

import java.io.File;
import java.util.HashMap;

import org.openlca.commons.Res;
import org.openlca.io.olca.migration.MatchingStrategy;
import org.openlca.io.olca.migration.MigrationPlan;

class MatchDump {

	private MatchDump() {
	}

	static Res<MatchDumpStats> apply(MigrationPlan plan, File file) {
		if (plan == null || file == null)
			return Res.error("Plan or file is null");

		var json = readObject(file);
		if (json.isEmpty())
			return Res.error("Failed to read file: " + file);

		var root = json.get();
		var matchesArray = getArray(root, "matches");
		if (matchesArray == null || matchesArray.isEmpty())
			return Res.error("No matches found in file");

		// build index: source provider refId -> dump entry
		var dumpIdx = new HashMap<String, DumpEntry>();
		for (var elem : matchesArray) {
			if (!elem.isJsonObject())
				continue;
			var matchObj = elem.getAsJsonObject();

			var sourceObj = getObject(matchObj, "source");
			if (sourceObj == null)
				continue;

			var providerObj = getObject(sourceObj, "provider");
			if (providerObj == null)
				continue;

			var sourceRefId = getString(providerObj, "@id");
			if (sourceRefId == null)
				continue;

			var strategy = getEnum(matchObj,
				"strategy", MatchingStrategy.class);
			var selectedRefId = getString(matchObj, "selected");
			if (selectedRefId != null) {
				dumpIdx.put(sourceRefId,
					new DumpEntry(strategy, selectedRefId));
			}
		}

		int checked = plan.providerMatches().size();
		int matching = 0;
		int updated = 0;

		for (var match : plan.providerMatches()) {
			if (match.source() == null
				|| match.source().provider() == null)
				continue;

			var sourceRefId = match.source().provider().refId;
			var entry = dumpIdx.get(sourceRefId);
			if (entry == null)
				continue;

			matching++;

			for (var alt : match.alternatives()) {
				if (alt.provider() == null
					|| !alt.provider().refId.equals(entry.selectedRefId))
					continue;

				// found the matching alternative
				if (alt != match.selected()) {
					match.select(alt, entry.strategy);
					updated++;
				}
				break;
			}
		}

		return Res.ok(new MatchDumpStats(checked, matching, updated));
	}

	private record DumpEntry(
		MatchingStrategy strategy,
		String selectedRefId
	) {
	}

}
