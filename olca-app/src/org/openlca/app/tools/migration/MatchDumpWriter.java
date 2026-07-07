package org.openlca.app.tools.migration;

import java.io.File;

import org.openlca.commons.Res;
import org.openlca.io.olca.migration.MigrationPlan;
import org.openlca.io.olca.migration.ProviderMatch;
import org.openlca.jsonld.Json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class MatchDumpWriter {

	private final MigrationPlan plan;
	private final File file;

	private MatchDumpWriter(MigrationPlan plan, File file) {
		this.plan = plan;
		this.file = file;
	}

	static Res<Void> write(MigrationPlan plan, File file) {
		return plan != null && file != null
			? new MatchDumpWriter(plan, file).write()
			: Res.error("Plan or file is null");
	}

	private Res<Void> write() {
		try {
			var root = new JsonObject();
			var matches = new JsonArray();
			for (var match : plan.providerMatches()) {
				var obj = matchObjectOf(match);
				if (obj != null) {
					matches.add(obj);
				}
			}
			Json.put(root, "matches", matches);
			Json.write(root, file);
			return Res.ok();
		} catch (Exception e) {
			return Res.error("Failed to store matches", e);
		}
	}

	private JsonObject matchObjectOf(ProviderMatch match) {
		if (match == null || !match.isComplete())
			return null;
		var obj = new JsonObject();
		Json.put(obj, "provider", match.source().provider().refId);
		Json.put(obj, "flow", match.source().flow().refId);
		Json.put(obj, "selected", match.selected().provider().refId);
		Json.put(obj, "strategy", match.strategy());
		return obj;
	}
}
