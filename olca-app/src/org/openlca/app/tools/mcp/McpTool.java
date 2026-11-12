package org.openlca.app.tools.mcp;

import java.util.Objects;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// A tool of the openLCA MCP server: its definition (name, description, input
/// schema, annotations) and the handler that is called when an agent uses the
/// tool. The definitions are stored as JSON files in the `tools` resource
/// folder of this package, see [McpTools].
///
/// @param definition the tool definition as sent to the agents
/// @param handler    the function that executes the tool
public record McpTool(McpSchema.Tool definition, Handler handler) {

	public McpTool {
		Objects.requireNonNull(definition);
		Objects.requireNonNull(handler);
	}

	public String name() {
		return definition.name();
	}

	/// The function that executes a tool call.
	@FunctionalInterface
	public interface Handler {
		CallToolResult call(ToolArgs args);
	}
}
