package org.openlca.app.tools.mcp.tools;

import org.openlca.commons.Strings;
import org.openlca.core.services.JsonRef;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Creates a new data set or replaces an existing one. The data set is given
/// in the openLCA JSON-LD format and must contain the `@type` attribute.
class SaveDataTool implements McpTool {

	private final ToolContext ctx;

	SaveDataTool(ToolContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String name() {
		return "save_data";
	}

	@Override
	public CallToolResult call(ToolArgs args) {
		var json = args.json("data").orElse(null);
		if (json == null)
			return ToolResults.error("no data set given in 'data'");
		var type = JsonRef.typeOf(json);
		if (type == null)
			return ToolResults.error(
				"the data set has no valid @type, e.g. \"@type\": \"Process\"");
		var clazz = type.getModelClass();
		var db = ctx.db();

		// check if an existing data set can be modified
		var id = JsonRef.idOf(json);
		if (Strings.isNotBlank(id)) {
			var existing = db.getDescriptor(clazz, id);
			var err = existing != null
				? ctx.sync().checkModifiable(existing)
				: null;
			if (err != null)
				return ToolResults.error(err);
		}

		var r = ctx.data().put(clazz, json);
		if (r.isError())
			return ToolResults.error(r.error());
		var saved = db.getDescriptor(clazz, JsonRef.idOf(r.value()));
		if (saved != null) {
			ctx.sync().saved(saved);
		}
		return ToolResults.json(r.value());
	}
}
