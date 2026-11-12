package org.openlca.app.tools.mcp.tools;

import org.openlca.core.model.ProductSystem;
import org.openlca.core.services.JsonRef;

import com.google.gson.JsonObject;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Creates a new product system for a process and links the product inputs
/// and waste outputs of the processes automatically to providers.
class CreateProductSystemTool implements McpTool {

	private final ToolContext ctx;

	CreateProductSystemTool(ToolContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String name() {
		return "create_product_system";
	}

	@Override
	public CallToolResult call(ToolArgs args) {
		var processId = args.string("processId").orElse(null);
		if (processId == null)
			return ToolResults.error("the @id of a process is required");

		var config = new JsonObject();
		config.addProperty("providerLinking",
			args.string("providerLinking").orElse("PREFER_DEFAULTS"));
		config.addProperty("preferUnitProcesses",
			args.bool("preferUnitProcesses", false));
		args.number("cutoff").ifPresent(c -> config.addProperty("cutoff", c));

		try {
			var r = ctx.data().createProductSystem(processId, config);
			if (r.isError())
				return ToolResults.error(r.error());
			var system = ctx.db().getDescriptor(
				ProductSystem.class, JsonRef.idOf(r.value()));
			if (system != null) {
				ctx.sync().saved(system);
			}
			return ToolResults.json(r.value());
		} catch (Exception e) {
			return ToolResults.error(
				"failed to create product system: " + e.getMessage());
		}
	}
}
