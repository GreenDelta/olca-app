package org.openlca.app.tools.mcp;

import java.util.Objects;
import java.util.function.Function;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Defines a tool of the openLCA MCP server: its metadata, the JSON schema of
/// its input, and the function that is called when an agent uses the tool.
///
/// The tool definitions are independent of the MCP transport and the UI, so
/// that they can be registered, filtered, or configured independently of the
/// server that exposes them.
///
/// @param name        the unique name of the tool, e.g. `search_data`
/// @param title       a short, human-readable title of the tool
/// @param description the description of the tool for the agent; it should
///                    explain when and how to use the tool
/// @param inputSchema the JSON schema of the tool arguments as JSON string
/// @param readOnly    `true` if the tool does not modify any data
/// @param destructive `true` if the tool can delete or overwrite data
/// @param handler     the function that executes the tool
public record McpTool(
		String name,
		String title,
		String description,
		String inputSchema,
		boolean readOnly,
		boolean destructive,
		Function<ToolArgs, CallToolResult> handler) {

	public McpTool {
		Objects.requireNonNull(name);
		Objects.requireNonNull(inputSchema);
		Objects.requireNonNull(handler);
	}

	public static Builder of(String name) {
		return new Builder(name);
	}

	public static final class Builder {

		private final String name;
		private String title;
		private String description;
		private String inputSchema = """
				{"type": "object", "properties": {}}""";
		private boolean readOnly = true;
		private boolean destructive;

		private Builder(String name) {
			this.name = name;
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder inputSchema(String inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		/// Marks the tool as a tool that creates or updates data.
		public Builder writes() {
			this.readOnly = false;
			return this;
		}

		/// Marks the tool as a tool that can delete or overwrite data.
		public Builder destructive() {
			this.readOnly = false;
			this.destructive = true;
			return this;
		}

		public McpTool handler(Function<ToolArgs, CallToolResult> handler) {
			return new McpTool(
					name, title, description, inputSchema, readOnly, destructive, handler);
		}
	}
}
