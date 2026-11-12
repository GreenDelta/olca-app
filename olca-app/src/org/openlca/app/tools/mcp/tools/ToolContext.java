package org.openlca.app.tools.mcp.tools;

import org.openlca.core.database.IDatabase;
import org.openlca.core.services.JsonDataService;

/// The shared context of the tools: the database, the JSON-LD data service of
/// that database, and the synchronization of data changes.
///
/// @param db   the database of the server
/// @param data the JSON-LD data service, the same as used by the IPC server
/// @param sync the synchronization of data changes, e.g. with the UI
record ToolContext(IDatabase db, JsonDataService data, DataSync sync) {

	static ToolContext of(IDatabase db, DataSync sync) {
		return new ToolContext(
			db, new JsonDataService(db), sync != null ? sync : DataSync.NONE);
	}
}
