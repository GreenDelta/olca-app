package org.openlca.app.tools.mcp.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.openlca.commons.Strings;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.jsonld.Json;
import org.openlca.jsonld.output.JsonRefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Searches data sets by name and returns compact references. All words of the
/// query must occur in the name (case-insensitive); the results are ranked by
/// exact matches, prefix matches, and name length.
class SearchTool implements McpTool {

	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 500;

	private final ToolContext ctx;

	SearchTool(ToolContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String name() {
		return "search_data";
	}

	@Override
	public CallToolResult call(ToolArgs args) {
		var typeArg = args.string("type");
		var type = args.modelType("type");
		if (typeArg.isPresent() && type.isEmpty())
			return ToolResults.error("unknown data set type: " + typeArg.get());

		var types = type.map(List::of)
			.orElseGet(() -> List.of(ModelType.values()));
		var query = args.string("query").orElse("");
		var terms = Arrays.stream(query.toLowerCase(Locale.ROOT).split("\\s+"))
			.filter(Strings::isNotBlank)
			.toList();
		var categoryPrefix = args.string("category")
			.map(s -> s.toLowerCase(Locale.ROOT))
			.orElse(null);
		int limit = Math.clamp(args.integer("limit", DEFAULT_LIMIT), 1, MAX_LIMIT);
		int offset = Math.max(0, args.integer("offset", 0));

		var db = ctx.db();
		var refs = JsonRefs.of(db).withLibraryFields(true);
		var matches = new ArrayList<Match>();
		for (var t : types) {
			for (var d : db.getDescriptors(t.getModelClass())) {
				int rank = rankOf(d, query, terms);
				if (rank < 0)
					continue;
				var ref = refs.asRef(d);
				if (categoryPrefix != null) {
					var path = Json.getString(ref, "category");
					if (path == null
						|| !path.toLowerCase(Locale.ROOT).startsWith(categoryPrefix))
						continue;
				}
				matches.add(new Match(rank, d.name != null ? d.name : "", ref));
			}
		}

		matches.sort(Comparator
			.comparingInt(Match::rank)
			.thenComparingInt(m -> m.name().length())
			.thenComparing(Match::name, String.CASE_INSENSITIVE_ORDER));

		var items = new JsonArray();
		for (int i = offset; i < matches.size() && items.size() < limit; i++) {
			items.add(matches.get(i).ref());
		}
		var result = new JsonObject();
		result.addProperty("total", matches.size());
		result.addProperty("offset", offset);
		result.add("items", items);
		return ToolResults.json(result);
	}

	/// Returns the rank of the descriptor for the given query (lower is
	/// better) or `-1` if it does not match.
	private static int rankOf(RootDescriptor d, String query, List<String> terms) {
		if (terms.isEmpty())
			return 3;
		if (query.equals(d.refId))
			return 0;
		if (d.name == null)
			return -1;
		var name = d.name.toLowerCase(Locale.ROOT);
		for (var term : terms) {
			if (!name.contains(term))
				return -1;
		}
		var q = query.toLowerCase(Locale.ROOT);
		if (name.equals(q))
			return 0;
		if (name.startsWith(q))
			return 1;
		return 2;
	}

	private record Match(int rank, String name, JsonObject ref) {
	}
}
