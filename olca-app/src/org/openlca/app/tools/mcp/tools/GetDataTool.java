package org.openlca.app.tools.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Returns the complete data set with a given type and ID (or name) in the
/// openLCA JSON-LD format.
class GetDataTool implements McpTool {

	private final ToolContext ctx;

	GetDataTool(ToolContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String name() {
		return "get_data";
	}

	@Override
	public CallToolResult call(ToolArgs args) {
		var type = args.modelType("type").orElse(null);
		if (type == null)
			return ToolResults.error("a valid data set type is required");
		var clazz = type.getModelClass();

		var id = args.string("id");
		if (id.isPresent()) {
			var r = ctx.data().get(clazz, id.get());
			return ToolResults.of(r, ToolResults.notFoundMessage(type, id.get()));
		}

		var name = args.string("name");
		if (name.isPresent()) {
			var r = ctx.data().getForName(clazz, name.get());
			return ToolResults.of(r, "no " + ToolArgs.typeNameOf(type)
				+ " with name=" + name.get());
		}
		return ToolResults.error("an @id or name is required");
	}
}
