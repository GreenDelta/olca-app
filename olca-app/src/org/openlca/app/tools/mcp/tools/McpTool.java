package org.openlca.app.tools.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// A tool of the openLCA MCP server. The definition of a tool (title,
/// description, input schema, annotations) is stored in the JSON file
/// `schemas/<name>.json` of this package and registered together with the
/// tool in [McpTools].
public interface McpTool {

	/// The unique name of the tool, e.g. `search_data`, which is also the
	/// name of its definition file.
	String name();

	/// Executes the tool with the given arguments.
	CallToolResult call(ToolArgs args);
}
