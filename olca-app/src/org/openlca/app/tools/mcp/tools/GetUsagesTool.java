package org.openlca.app.tools.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Returns references to the data sets that use a given data set.
class GetUsagesTool implements McpTool {

	private final ToolContext ctx;

	GetUsagesTool(ToolContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String name() {
		return "get_usages";
	}

	@Override
	public CallToolResult call(ToolArgs args) {
		var type = args.modelType("type").orElse(null);
		var id = args.string("id").orElse(null);
		if (type == null || id == null)
			return ToolResults.error("a valid data set type and @id are required");
		var r = ctx.data().getUsageOf(type.getModelClass(), id);
		return ToolResults.of(r, ToolResults.notFoundMessage(type, id));
	}
}
