package org.openlca.app.tools.mcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

/// Tools for searching, reading, creating, updating, and deleting data sets
/// of the database. They are based on the same JSON-LD services as the IPC
/// server, so the data format is the openLCA schema (olca-schema).
public final class DataTools {

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

	public static List<McpTool> of(IDatabase db, DataSync sync) {
		var tools = new DataTools(db, sync);
		return List.of(
				tools.searchData(),
				tools.getData(),
				tools.saveData(),
				tools.deleteData(),
				tools.getProviders(),
				tools.getUsages(),
				tools.getParameters(),
				tools.createProductSystem());
	}

	/// The JSON schema snippet of the `type` property, with all model types as
	/// enumeration values.
	private static String typeProperty(String description) {
		var names = Arrays.stream(ModelType.values())
				.map(t -> "\"" + ToolArgs.typeNameOf(t) + "\"")
				.collect(Collectors.joining(", "));
		return """
				"type": {
					"type": "string",
					"enum": [%s],
					"description": "%s"
				}""".formatted(names, description);
	}

	// region: search

	private McpTool searchData() {
		return McpTool.of("search_data")
				.title("Search data sets")
				.description("""
						Searches data sets in the openLCA database by name and \
						returns compact references (@type, @id, name, category, and \
						type specific fields like flowType, refUnit, or location). \
						Use this to find existing flows, processes, units, locations \
						etc. before creating new data sets; new data sets should \
						reference existing ones wherever possible. All words of the \
						query must occur in the name (case-insensitive). Results are \
						ranked: exact matches first, then names starting with the \
						query, then shorter names.""")
				.inputSchema("""
						{
							"type": "object",
							"properties": {
								%s,
								"query": {
									"type": "string",
									"description": "Words that must occur in the name; if empty, all data sets of the type are listed. An exact @id is matched too."
								},
								"category": {
									"type": "string",
									"description": "Optional category path prefix, e.g. 'Elementary flows/Emission to air'."
								},
								"limit": {
									"type": "integer",
									"description": "Maximum number of results (default 50, max 500)."
								},
								"offset": {
									"type": "integer",
									"description": "Number of results to skip, for paging (default 0)."
								}
							}
						}""".formatted(typeProperty(
						"The data set type to search; if omitted, all types are searched.")))
				.handler(this::onSearch);
	}

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

	private McpTool getData() {
		return McpTool.of("get_data")
				.title("Get a data set")
				.description("""
						Returns the complete data set with the given type and @id \
						(or name, if no @id is given) in the openLCA JSON-LD format. \
						Use this to inspect a data set in detail or as a template \
						for creating similar data sets.""")
				.inputSchema("""
						{
							"type": "object",
							"properties": {
								%s,
								"id": {
									"type": "string",
									"description": "The @id of the data set."
								},
								"name": {
									"type": "string",
									"description": "The name of the data set; used when no @id is given."
								}
							},
							"required": ["type"]
						}""".formatted(typeProperty("The type of the data set.")))
				.handler(this::onGet);
	}

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

	private McpTool saveData() {
		return McpTool.of("save_data")
				.title("Create or update a data set")
				.description("""
						Creates a new data set or updates an existing one. The data \
						set is given in the openLCA JSON-LD format (olca-schema) and \
						must contain the @type attribute. If no @id is given, a new \
						one is generated. If a data set with the given @id exists, \
						it is replaced completely by the given data: to update a \
						data set, get it first, modify it, and save the complete \
						data set. References to other data sets are given as \
						objects with @type and @id, e.g. \
						{"@type": "Flow", "@id": "..."}; these data sets must exist \
						in the database. Categories are given as paths, e.g. \
						"category": "Materials/Metals", and are created if they do \
						not exist. Returns a reference to the saved data set.""")
				.inputSchema("""
						{
							"type": "object",
							"properties": {
								"data": {
									"type": "object",
									"description": "The data set in openLCA JSON-LD format, including @type."
								}
							},
							"required": ["data"]
						}""")
				.writes()
				.handler(this::onSave);
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

	private McpTool deleteData() {
		return McpTool.of("delete_data")
				.title("Delete a data set")
				.description("""
						Deletes the data set with the given type and @id from the \
						database. Use get_usages first to check that the data set \
						is not used by other data sets. Data sets from libraries \
						cannot be deleted.""")
				.inputSchema("""
						{
							"type": "object",
							"properties": {
								%s,
								"id": {
									"type": "string",
									"description": "The @id of the data set."
								}
							},
							"required": ["type", "id"]
						}""".formatted(typeProperty("The type of the data set.")))
				.destructive()
				.handler(this::onDelete);
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

	private McpTool getProviders() {
		return McpTool.of("get_providers")
				.title("Get providers of a flow")
				.description("""
						Returns the processes (and product systems or results) that \
						produce the given product or treat the given waste flow. Use \
						this to find providers for the product inputs and waste \
						outputs of a process, e.g. to set default providers.""")
				.inputSchema("""
						{
							"type": "object",
							"properties": {
								"flowId": {
									"type": "string",
									"description": "The @id of the product or waste flow."
								}
							},
							"required": ["flowId"]
						}""")
				.handler(this::onGetProviders);
	}

	private CallToolResult onGetProviders(ToolArgs args) {
		var flowId = args.string("flowId").orElse(null);
		if (flowId == null)
			return ToolResults.error("the @id of a flow is required");
		var r = service.getProvidersOfFlow(flowId);
		return ToolResults.of(r, "no flow with @id=" + flowId);
	}

	private McpTool getUsages() {
		return McpTool.of("get_usages")
				.title("Get usages of a data set")
				.description("""
						Returns references to the data sets that use the data set \
						with the given type and @id, e.g. the processes in which a \
						flow is used.""")
				.inputSchema("""
						{
							"type": "object",
							"properties": {
								%s,
								"id": {
									"type": "string",
									"description": "The @id of the data set."
								}
							},
							"required": ["type", "id"]
						}""".formatted(typeProperty("The type of the data set.")))
				.handler(this::onGetUsages);
	}

	private CallToolResult onGetUsages(ToolArgs args) {
		var type = args.modelType("type").orElse(null);
		var id = args.string("id").orElse(null);
		if (type == null || id == null)
			return ToolResults.error("a valid data set type and @id are required");
		var r = service.getUsageOf(type.getModelClass(), id);
		return ToolResults.of(r, "no " + ToolArgs.typeNameOf(type) + " with @id=" + id);
	}

	private McpTool getParameters() {
		return McpTool.of("get_parameters")
				.title("Get parameters of a data set")
				.description("""
						Returns the parameters of a process, impact category, or \
						product system (including the parameter redefinitions of the \
						product system).""")
				.inputSchema("""
						{
							"type": "object",
							"properties": {
								"type": {
									"type": "string",
									"enum": ["Process", "ImpactCategory", "ProductSystem"],
									"description": "The type of the data set."
								},
								"id": {
									"type": "string",
									"description": "The @id of the data set."
								}
							},
							"required": ["type", "id"]
						}""")
				.handler(this::onGetParameters);
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

	private McpTool createProductSystem() {
		return McpTool.of("create_product_system")
				.title("Create a product system")
				.description("""
						Creates a new product system for the given process. The \
						quantitative reference of the process becomes the reference \
						flow of the product system, and the product inputs and waste \
						outputs of the processes are linked automatically to \
						providers in the database. The process must have a \
						quantitative reference. Returns a reference to the new \
						product system.""")
				.inputSchema("""
						{
							"type": "object",
							"properties": {
								"processId": {
									"type": "string",
									"description": "The @id of the reference process."
								},
								"providerLinking": {
									"type": "string",
									"enum": ["IGNORE_DEFAULTS", "PREFER_DEFAULTS", "ONLY_DEFAULTS"],
									"description": "How default providers of the processes are considered in the linking (default: PREFER_DEFAULTS)."
								},
								"preferUnitProcesses": {
									"type": "boolean",
									"description": "If true, unit processes are preferred over system processes when multiple providers exist (default: false)."
								},
								"cutoff": {
									"type": "number",
									"description": "Optional cutoff value for the linking."
								}
							},
							"required": ["processId"]
						}""")
				.writes()
				.handler(this::onCreateProductSystem);
	}

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
