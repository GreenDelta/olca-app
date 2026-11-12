package org.openlca.app.tools.mcp.tools;

import org.openlca.core.model.ModelType;
import org.openlca.core.services.Response;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Factory methods for tool results. Results are returned as compact JSON
/// text, which keeps the token usage of the agents low.
public final class ToolResults {

	private static final Gson gson = new Gson();

	private ToolResults() {
	}

	public static CallToolResult json(JsonElement json) {
		return text(gson.toJson(json));
	}

	public static CallToolResult text(String text) {
		return CallToolResult.builder()
				.addTextContent(text)
				.isError(false)
				.build();
	}

	public static CallToolResult error(String message) {
		return CallToolResult.builder()
				.addTextContent(message)
				.isError(true)
				.build();
	}

	/// Returns an error that the data set of the given type and ID does not
	/// exist.
	public static CallToolResult notFound(ModelType type, String id) {
		return error(notFoundMessage(type, id));
	}

	static String notFoundMessage(ModelType type, String id) {
		return "no " + ToolArgs.typeNameOf(type) + " with @id=" + id;
	}

	/// Converts a response of the openLCA JSON services into a tool result. An
	/// empty response is converted into an error with the given message, as an
	/// agent needs a clear answer when something was not found.
	public static CallToolResult of(
			Response<? extends JsonElement> r, String notFoundMessage) {
		if (r.isError())
			return error(r.error());
		if (r.isEmpty())
			return error(notFoundMessage);
		return json(r.value());
	}
}
