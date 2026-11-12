package org.openlca.app.tools.mcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.services.JsonDataService;
import org.openlca.core.services.JsonRef;
import org.openlca.jsonld.Json;
import org.openlca.jsonld.output.JsonRefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/// Handlers of the tools for searching, reading, creating, updating, and
/// deleting data sets of the database. They are based on the same JSON-LD
/// services as the IPC server, so the data format is the openLCA schema
/// (olca-schema). The tool definitions are stored in the JSON files of the
/// `tools` resource folder.
final class DataTools {

	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 500;

	private final IDatabase db;
	private final JsonDataService service;
	private final DataSync sync;

	private DataTools(IDatabase db, DataSync sync) {
		this.db = db;
		this.service = new JsonDataService(db);
		this.sync = sync != null ? sync : DataSync.NONE;
	}

	/// Returns the tool handlers by tool name. The definitions of these tools
	/// are stored in the respective JSON files, see [McpTools].
	static Map<String, McpTool.Handler> handlersOf(IDatabase db, DataSync sync) {
		var tools = new DataTools(db, sync);
		var handlers = new LinkedHashMap<String, McpTool.Handler>();
		handlers.put("search_data", tools::onSearch);
		handlers.put("get_data", tools::onGet);
		handlers.put("save_data", tools::onSave);
		handlers.put("delete_data", tools::onDelete);
		handlers.put("get_providers", tools::onGetProviders);
		handlers.put("get_usages", tools::onGetUsages);
		handlers.put("get_parameters", tools::onGetParameters);
		handlers.put("create_product_system", tools::onCreateProductSystem);
		return handlers;
	}

	// region: search

	private CallToolResult onSearch(ToolArgs args) {
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

	// endregion

	// region: get, save, delete

	private CallToolResult onGet(ToolArgs args) {
		var type = args.modelType("type").orElse(null);
		if (type == null)
			return ToolResults.error("a valid data set type is required");
		var id = args.string("id");
		if (id.isPresent()) {
			var r = service.get(type.getModelClass(), id.get());
			return ToolResults.of(r, "no " + ToolArgs.typeNameOf(type) + " with @id=" + id.get());
		}
		var name = args.string("name");
		if (name.isPresent()) {
			var r = service.getForName(type.getModelClass(), name.get());
			return ToolResults.of(r, "no " + ToolArgs.typeNameOf(type) + " with name=" + name.get());
		}
		return ToolResults.error("an @id or name is required");
	}

	private CallToolResult onSave(ToolArgs args) {
		var json = args.json("data").orElse(null);
		if (json == null)
			return ToolResults.error("no data set given in 'data'");
		var type = JsonRef.typeOf(json);
		if (type == null)
			return ToolResults.error(
					"the data set has no valid @type, e.g. \"@type\": \"Process\"");

		// check if an existing data set can be modified
		var id = JsonRef.idOf(json);
		if (Strings.isNotBlank(id)) {
			var existing = db.getDescriptor(type.getModelClass(), id);
			var err = existing != null
					? sync.checkModifiable(existing)
					: null;
			if (err != null)
				return ToolResults.error(err);
		}

		var r = service.put(type.getModelClass(), json);
		if (r.isError())
			return ToolResults.error(r.error());
		var savedId = JsonRef.idOf(r.value());
		var saved = db.getDescriptor(type.getModelClass(), savedId);
		if (saved != null) {
			sync.saved(saved);
		}
		return ToolResults.json(r.value());
	}

	private CallToolResult onDelete(ToolArgs args) {
		var type = args.modelType("type").orElse(null);
		var id = args.string("id").orElse(null);
		if (type == null || id == null)
			return ToolResults.error("a valid data set type and @id are required");
		var d = db.getDescriptor(type.getModelClass(), id);
		if (d == null)
			return ToolResults.error("no " + ToolArgs.typeNameOf(type) + " with @id=" + id);
		var err = sync.checkModifiable(d);
		if (err != null)
			return ToolResults.error(err);
		var r = service.delete(type.getModelClass(), id);
		if (r.isError())
			return ToolResults.error(r.error());
		sync.deleted(d);
		return ToolResults.of(r, "no " + ToolArgs.typeNameOf(type) + " with @id=" + id);
	}

	// endregion

	// region: related data

	private CallToolResult onGetProviders(ToolArgs args) {
		var flowId = args.string("flowId").orElse(null);
		if (flowId == null)
			return ToolResults.error("the @id of a flow is required");
		var r = service.getProvidersOfFlow(flowId);
		return ToolResults.of(r, "no flow with @id=" + flowId);
	}

	private CallToolResult onGetUsages(ToolArgs args) {
		var type = args.modelType("type").orElse(null);
		var id = args.string("id").orElse(null);
		if (type == null || id == null)
			return ToolResults.error("a valid data set type and @id are required");
		var r = service.getUsageOf(type.getModelClass(), id);
		return ToolResults.of(r, "no " + ToolArgs.typeNameOf(type) + " with @id=" + id);
	}

	private CallToolResult onGetParameters(ToolArgs args) {
		var type = args.modelType("type").orElse(null);
		var id = args.string("id").orElse(null);
		if (type == null || id == null)
			return ToolResults.error("a valid data set type and @id are required");
		var r = service.getParametersOf(type.getModelClass(), id);
		return ToolResults.of(r, "no " + ToolArgs.typeNameOf(type) + " with @id=" + id);
	}

	// endregion

	// region: product systems

	private CallToolResult onCreateProductSystem(ToolArgs args) {
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
			var r = service.createProductSystem(processId, config);
			if (r.isError())
				return ToolResults.error(r.error());
			var system = db.getDescriptor(
					ProductSystem.class, JsonRef.idOf(r.value()));
			if (system != null) {
				sync.saved(system);
			}
			return ToolResults.json(r.value());
		} catch (Exception e) {
			return ToolResults.error(
					"failed to create product system: " + e.getMessage());
		}
	}

	// endregion
}
