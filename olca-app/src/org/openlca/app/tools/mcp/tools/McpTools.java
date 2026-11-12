package org.openlca.app.tools.mcp.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
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
/// the resource folder `schemas` of this package, one file per tool named
/// `<tool name>.json`.
public final class McpTools {

	/// A tool together with its definition.
	public record Spec(McpSchema.Tool definition, McpTool tool) {
	}

	private McpTools() {
	}

	/// Returns the tools of the server for the given database. Throws an
	/// exception if a tool definition is missing or invalid, so that such
	/// errors are detected when the server is started.
	public static List<Spec> of(
		IDatabase db, DataSync sync, McpJsonMapper json
	) {
		var ctx = ToolContext.of(db, sync);
		var tools = List.of(
			new SearchTool(ctx),
			new GetDataTool(ctx),
			new SaveDataTool(ctx),
			new DeleteDataTool(ctx),
			new GetProvidersTool(ctx),
			new GetUsagesTool(ctx),
			new GetParametersTool(ctx),
			new CreateProductSystemTool(ctx));

		var specs = new ArrayList<Spec>(tools.size());
		for (var tool : tools) {
			specs.add(new Spec(definitionOf(tool.name(), json), tool));
		}
		return specs;
	}

	private static McpSchema.Tool definitionOf(String name, McpJsonMapper json) {
		var path = "schemas/" + name + ".json";
		McpSchema.Tool tool;
		try (var stream = McpTools.class.getResourceAsStream(path)) {
			if (stream == null)
				throw new IllegalStateException(
					"no definition found for MCP tool '" + name + "': " + path);
			tool = json.readValue(stream.readAllBytes(), McpSchema.Tool.class);
		} catch (IOException e) {
			throw new UncheckedIOException(
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
}
