package org.openlca.app.tools.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Returns the parameters of a process, impact category, or product system.
class GetParametersTool implements McpTool {

	private final ToolContext ctx;

	GetParametersTool(ToolContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String name() {
		return "get_parameters";
	}

	@Override
	public CallToolResult call(ToolArgs args) {
		var type = args.modelType("type").orElse(null);
		var id = args.string("id").orElse(null);
		if (type == null || id == null)
			return ToolResults.error("a valid data set type and @id are required");
		var r = ctx.data().getParametersOf(type.getModelClass(), id);
		return ToolResults.of(r, ToolResults.notFoundMessage(type, id));
	}
}
