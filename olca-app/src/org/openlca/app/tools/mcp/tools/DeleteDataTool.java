package org.openlca.app.tools.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Deletes the data set with a given type and ID from the database.
class DeleteDataTool implements McpTool {

	private final ToolContext ctx;

	DeleteDataTool(ToolContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String name() {
		return "delete_data";
	}

	@Override
	public CallToolResult call(ToolArgs args) {
		var type = args.modelType("type").orElse(null);
		var id = args.string("id").orElse(null);
		if (type == null || id == null)
			return ToolResults.error("a valid data set type and @id are required");
		var clazz = type.getModelClass();

		var d = ctx.db().getDescriptor(clazz, id);
		if (d == null)
			return ToolResults.notFound(type, id);
		var err = ctx.sync().checkModifiable(d);
		if (err != null)
			return ToolResults.error(err);

		var r = ctx.data().delete(clazz, id);
		if (r.isError())
			return ToolResults.error(r.error());
		ctx.sync().deleted(d);
		return ToolResults.of(r, ToolResults.notFoundMessage(type, id));
	}
}
