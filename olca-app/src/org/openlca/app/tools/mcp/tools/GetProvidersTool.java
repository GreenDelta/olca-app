package org.openlca.app.tools.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Returns the providers (processes, product systems, results) of a product
/// or waste flow.
class GetProvidersTool implements McpTool {

	private final ToolContext ctx;

	GetProvidersTool(ToolContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String name() {
		return "get_providers";
	}

	@Override
	public CallToolResult call(ToolArgs args) {
		var flowId = args.string("flowId").orElse(null);
		if (flowId == null)
			return ToolResults.error("the @id of a flow is required");
		var r = ctx.data().getProvidersOfFlow(flowId);
		return ToolResults.of(r, "no flow with @id=" + flowId);
	}
}
