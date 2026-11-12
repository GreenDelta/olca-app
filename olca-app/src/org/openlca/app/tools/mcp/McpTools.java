package org.openlca.app.tools.mcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.database.IDatabase;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;

/// The registry of the tools that are available in the openLCA MCP server.
/// This is the place where tools can be filtered or configured later, e.g. to
/// only allow read-only tools.
///
/// The tool definitions are stored as JSON files in the format of the MCP
/// tool definition (name, title, description, inputSchema, annotations) in
/// the resource folder `tools` of this package, one file per tool named
/// `<tool name>.json`. In the code, only the handlers are registered for the
/// tool names. The general instructions for the agents are stored in the
/// resource file `instructions.md`.
public final class McpTools {

	private McpTools() {
	}

	/// Returns the tools of the server for the given database. Throws an
	/// exception if a tool definition is missing or invalid, so that such
	/// errors are detected when the server is started.
	public static List<McpTool> of(
			IDatabase db, DataSync sync, McpJsonMapper json) {
		var handlers = new LinkedHashMap<String, McpTool.Handler>();
		handlers.putAll(DataTools.handlersOf(db, sync));

		var tools = new ArrayList<McpTool>(handlers.size());
		for (var e : handlers.entrySet()) {
			var definition = definitionOf(e.getKey(), json);
			tools.add(new McpTool(definition, e.getValue()));
		}
		return tools;
	}

	/// Returns the general instructions that are sent to the agent when it
	/// connects to the server. They describe the data model and the typical
	/// workflow.
	public static String instructions() {
		return new String(read("instructions.md"), StandardCharsets.UTF_8);
	}

	private static McpSchema.Tool definitionOf(String name, McpJsonMapper json) {
		var path = "tools/" + name + ".json";
		McpSchema.Tool tool;
		try {
			tool = json.readValue(read(path), McpSchema.Tool.class);
		} catch (IOException e) {
			throw new IllegalStateException(
					"invalid definition of MCP tool '" + name + "' in " + path, e);
		}
		if (!name.equals(tool.name()))
			throw new IllegalStateException("the definition in " + path
					+ " has a different tool name: " + tool.name());
		checkTypeEnum(tool);
		return tool;
	}

	/// The values of a `type` property are data set types, which are listed
	/// in the tool definitions. We check that they match the model types, as
	/// they could get out of sync when the model changes.
	private static void checkTypeEnum(McpSchema.Tool tool) {
		if (!(tool.inputSchema().get("properties") instanceof Map<?, ?> props)
				|| !(props.get("type") instanceof Map<?, ?> type)
				|| !(type.get("enum") instanceof List<?> values))
			return;
		for (var value : values) {
			if (ToolArgs.modelTypeOf(String.valueOf(value)) == null)
				throw new IllegalStateException("the definition of MCP tool '"
						+ tool.name() + "' contains an unknown data set type: " + value);
		}
	}

	private static byte[] read(String path) {
		try (var stream = McpTools.class.getResourceAsStream(path)) {
			if (stream == null)
				throw new IllegalStateException(
						"MCP resource not found: " + path);
			return stream.readAllBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(
					"failed to read MCP resource: " + path, e);
		}
	}
}
