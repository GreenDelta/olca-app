package org.openlca.app.collaboration.navigation.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.ModelType;
import org.openlca.git.util.TypeRefIdMap;
import org.openlca.git.util.TypedRefId;
import org.openlca.util.Strings;

class ModelReferences {

	private IDatabase database;
	private TypeRefIdMap<Long> refIdToId = new TypeRefIdMap<>();
	private EnumMap<ModelType, Map<Long, String>> idToRefId = new EnumMap<>(ModelType.class);
	private ReferenceMap references = new ReferenceMap();
	private ReferenceMap usages = new ReferenceMap();
	private Map<String, Long> nameToParameter = new HashMap<>();

	private ModelReferences(IDatabase database) {
		this.database = database;
	}

	public static ModelReferences scan(IDatabase database) {
		var refs = new ModelReferences(database);
		refs.init();
		refs.scan();
		return refs;
	}

	public Set<ModelReference> get(TypedRefId pair) {
		var refs = new HashSet<ModelReference>();
		refs.addAll(getReferences(pair));
		refs.addAll(getUsages(pair));
		return refs;
	}

	public Set<ModelReference> getReferences(TypedRefId pair) {
		return get(references, pair);
	}

	public Set<ModelReference> getUsages(TypedRefId pair) {
		return get(usages, pair);
	}

	private Set<ModelReference> get(ReferenceMap map, TypedRefId pair) {
		var refs = new HashSet<ModelReference>();
		var typeMap = map.get(pair.type);
		if (typeMap == null)
			return refs;
		var id = refIdToId.get(pair);
		var idMap = typeMap.get(id);
		if (idMap == null)
			return refs;
		idMap.keySet().forEach(targetType -> idMap.get(targetType)
				.forEach(targetId -> {
					var refId = idToRefId.get(targetType).get(targetId);
					if (!Strings.nullOrEmpty(refId)) {
						refs.add(new ModelReference(targetType, targetId, refId));
					}
				}));
		return refs;
	}

	private void init() {
		var query = "SELECT id, name FROM tbl_parameters WHERE scope = 'GLOBAL'";
		NativeSql.on(database).query(query, rs -> {
			nameToParameter.put(rs.getString(2), rs.getLong(1));
			return true;
		});
	}

	private void scan() {
		scanLocations();
		scanSources();
		scanActors();
		scanCurrencies();
		scanUnitGroups();
		scanFlowProperties();
		scanDQSystems();
		scanGlobalParameters();
		scanSocialIndicators();
		scanImpactCategories();
		scanImpactMethods();
		scanResults();
		scanEpds();
		scanFlows();
		scanProcesses();
		scanProductSystems();
		scanProjects();
	}

	private void scanLocations() {
		scanTable("tbl_locations", true,
				new ModelField(ModelType.LOCATION, "id"));
	}

	private void scanSources() {
		scanTable("tbl_sources", true,
				new ModelField(ModelType.SOURCE, "id"));
	}

	private void scanActors() {
		scanTable("tbl_actors", true,
				new ModelField(ModelType.ACTOR, "id"));
	}

	private void scanCurrencies() {
		scanTable("tbl_currencies", true,
				new ModelField(ModelType.CURRENCY, "id"),
				new ModelField(ModelType.CURRENCY, "f_reference_currency"));
	}

	private void scanUnitGroups() {
		scanTable("tbl_unit_groups", true,
				new ModelField(ModelType.UNIT_GROUP, "id"),
				new ModelField(ModelType.FLOW_PROPERTY, "f_default_flow_property"));
	}

	private void scanFlowProperties() {
		scanTable("tbl_flow_properties", true,
				new ModelField(ModelType.FLOW_PROPERTY, "id"),
				new ModelField(ModelType.UNIT_GROUP, "f_unit_group"));
	}

	private void scanDQSystems() {
		scanTable("tbl_dq_systems", true,
				new ModelField(ModelType.DQ_SYSTEM, "id"));
	}

	private void scanGlobalParameters() {
		var query = "SELECT id, ref_id FROM tbl_parameters WHERE scope = 'GLOBAL'";
		NativeSql.on(database).query(query, rs -> {
			var id = rs.getLong(1);
			var refId = rs.getString(2);
			putRefId(ModelType.PARAMETER, id, refId);
			return true;
		});
	}

	private void scanSocialIndicators() {
		scanTable("tbl_social_indicators", true,
				new ModelField(ModelType.SOCIAL_INDICATOR, "id"),
				new ModelField(ModelType.FLOW_PROPERTY, "f_activity_quantity"));
	}

	private void scanImpactCategories() {
		scanTable("tbl_impact_categories", true,
				new ModelField(ModelType.IMPACT_CATEGORY, "id"),
				new ModelField(ModelType.IMPACT_METHOD, "id"),
				new ModelField(ModelType.SOURCE, "f_source"));
		scanTable("tbl_impact_factors", false,
				new ModelField(ModelType.IMPACT_CATEGORY, "f_impact_category"),
				new ModelField(ModelType.FLOW, "f_flow"),
				new ModelField(ModelType.LOCATION, "f_location"));
	}

	private void scanImpactMethods() {
		scanTable("tbl_impact_methods", true,
				new ModelField(ModelType.IMPACT_METHOD, "id"),
				new ModelField(ModelType.SOURCE, "f_source"));
		scanTable("tbl_source_links", false,
				new ModelField(ModelType.IMPACT_METHOD, "f_owner"),
				new ModelField(ModelType.SOURCE, "f_source"));
		scanTable("tbl_impact_links", false,
				new ModelField(ModelType.IMPACT_METHOD, "f_impact_method"),
				new ModelField(ModelType.IMPACT_CATEGORY, "f_impact_category"));
	}

	private void scanFlows() {
		scanTable("tbl_flows", true,
				new ModelField(ModelType.FLOW, "id"));
		scanTable("tbl_flow_property_factors", false,
				new ModelField(ModelType.FLOW, "f_flow"),
				new ModelField(ModelType.FLOW_PROPERTY, "f_flow_property"));
	}

	private void scanProcesses() {
		var docsToProcess = scanTable("tbl_processes", true, "f_process_doc",
				new ModelField(ModelType.PROCESS, "id"),
				new ModelField(ModelType.LOCATION, "f_location"),
				new ModelField(ModelType.DQ_SYSTEM, "f_dq_system"),
				new ModelField(ModelType.DQ_SYSTEM, "f_exchange_dq_system"),
				new ModelField(ModelType.DQ_SYSTEM, "f_social_dq_system"));
		scanTable("tbl_process_docs", false,
				new ModelField(ModelType.PROCESS, "id", docsToProcess::get),
				new ModelField(ModelType.ACTOR, "f_reviewer"),
				new ModelField(ModelType.ACTOR, "f_data_documentor"),
				new ModelField(ModelType.ACTOR, "f_data_generator"),
				new ModelField(ModelType.ACTOR, "f_dataset_owner"),
				new ModelField(ModelType.SOURCE, "f_publication"));
		scanTable("tbl_source_links", false,
				new ModelField(ModelType.PROCESS, "f_owner", docsToProcess::get),
				new ModelField(ModelType.SOURCE, "f_source"));
		scanTable("tbl_exchanges", false,
				new ModelField(ModelType.PROCESS, "f_owner"),
				new ModelField(ModelType.PROCESS, "f_default_provider"),
				new ModelField(ModelType.FLOW, "f_flow"),
				new ModelField(ModelType.FLOW, "f_location"),
				new ModelField(ModelType.FLOW, "f_currency"));
	}

	private void scanProductSystems() {
		scanTable("tbl_product_systems", true,
				new ModelField(ModelType.PRODUCT_SYSTEM, "id"),
				new ModelField(ModelType.PROCESS, "f_reference_process"));
		scanTable("tbl_process_links", false,
				new ModelField(ModelType.PRODUCT_SYSTEM, "f_product_system"),
				new ModelField(ModelType.PROCESS, "f_process"),
				new ModelField(ModelType.PROCESS, "f_provider"));
		var setToSystem = scanTable("tbl_parameter_redef_sets", false, "id",
				new ModelField(ModelType.PRODUCT_SYSTEM, "f_product_system"));
		scanParameterRedefs(ModelType.PRODUCT_SYSTEM, setToSystem::get);
	}

	private void scanProjects() {
		scanTable("tbl_projects", true,
				new ModelField(ModelType.PROJECT, "id"),
				new ModelField(ModelType.IMPACT_METHOD, "f_impact_method"));
		var variantToProject = scanTable("tbl_project_variants", false, "id",
				new ModelField(ModelType.PROJECT, "f_project"),
				new ModelField(ModelType.PRODUCT_SYSTEM, "f_product_system"));
		scanParameterRedefs(ModelType.PROJECT, variantToProject::get);
	}

	private void scanEpds() {
		scanTable("tbl_epds", true,
				new ModelField(ModelType.EPD, "id"),
				new ModelField(ModelType.ACTOR, "f_manufacturer"),
				new ModelField(ModelType.ACTOR, "f_verifier"),
				new ModelField(ModelType.ACTOR, "f_program_operator"),
				new ModelField(ModelType.SOURCE, "f_pcr"),
				new ModelField(ModelType.FLOW, "f_flow"));
		scanTable("tbl_epd_modules", false,
				new ModelField(ModelType.EPD, "f_epd"),
				new ModelField(ModelType.RESULT, "f_result"));
	}

	private void scanResults() {
		scanTable("tbl_results", true,
				new ModelField(ModelType.RESULT, "id"),
				new ModelField(ModelType.PRODUCT_SYSTEM, "f_product_system"),
				new ModelField(ModelType.IMPACT_METHOD, "f_impact_method"));
		scanTable("tbl_flow_results", false,
				new ModelField(ModelType.RESULT, "f_result"),
				new ModelField(ModelType.FLOW, "f_flow"),
				new ModelField(ModelType.LOCATION, "f_location"));
		scanTable("tbl_impact_results", false,
				new ModelField(ModelType.RESULT, "f_result"),
				new ModelField(ModelType.IMPACT_CATEGORY, "f_impact_category"));
	}

	private void scanParameterRedefs(ModelType ownerType, Function<Long, Long> mediator) {
		var query = "SELECT f_owner,name FROM tbl_parameter_redefs WHERE context_type IS NULL";
		NativeSql.on(database).query(query, rs -> {
			var ownerId = rs.getLong(1);
			var actualOwnerId = mediator.apply(ownerId);
			if (actualOwnerId == null)
				return true;
			var name = rs.getString(2);
			var parameterId = nameToParameter.get(name);
			putRef(ownerType, ownerId, ModelType.PARAMETER, parameterId);
			return true;
		});
	}

	private void scanTable(String table, boolean isRootEntity, ModelField source, ModelField... targets) {
		scanTable(table, isRootEntity, null, source, targets);
	}

	/**
	 * if idField is not null, idField is queried additionally and a map between
	 * the value of source.field and value of idField is returned, otherwise an
	 * empty map
	 */
	private Map<Long, Long> scanTable(String table, boolean isRootEntity, String idField, ModelField source,
			ModelField... targets) {
		var targetFields = targets != null
				? Arrays.stream(targets).map(t -> t.field).toArray(n -> new String[n])
				: new String[0];
		var map = new HashMap<Long, Long>();
		query(table, isRootEntity, source, idField, targetFields, ids -> {
			var col = 0;
			var sourceId = ids[col++];
			if (idField != null) {
				map.put(ids[col++], sourceId);
			}
			if (source.mediator != null) {
				sourceId = source.mediator.apply(sourceId);
			}
			if (targets == null)
				return;
			for (var target : targets) {
				var targetId = ids[col++];
				if (targetId == 0l)
					continue;
				if (target.mediator != null) {
					targetId = target.mediator.apply(targetId);
				}
				putRef(source.type, sourceId, target.type, targetId);
			}
		});
		return map;
	}

	private void query(String table, boolean isRootEntity, ModelField sourceField, String idField, String[] targets,
			ResultHandler handler) {
		var fields = new ArrayList<String>();
		fields.add(sourceField.field);
		if (idField != null) {
			fields.add(idField);
		}
		fields.addAll(Arrays.asList(targets));
		var query = "SELECT " + fields.stream().collect(Collectors.joining(","))
				+ (isRootEntity ? ",ref_id " : "")
				+ " FROM " + table;
		NativeSql.on(database).query(query, rs -> {
			var ids = new long[fields.size()];
			for (var i = 0; i < fields.size(); i++) {
				ids[i] += rs.getLong(i + 1);
			}
			var id = ids[0];
			if (isRootEntity) {
				var refId = rs.getString(fields.size() + 1);
				putRefId(sourceField.type, id, refId);
			}
			handler.handle(ids);
			return true;
		});
	}

	private void putRef(ModelType sourceType, long sourceId, ModelType targetType, long targetId) {
		references.computeIfAbsent(sourceType, t -> new HashMap<>())
				.computeIfAbsent(sourceId, t -> new EnumMap<>(ModelType.class))
				.computeIfAbsent(targetType, t -> new HashSet<>())
				.add(targetId);
		usages.computeIfAbsent(targetType, t -> new HashMap<>())
				.computeIfAbsent(targetId, t -> new EnumMap<>(ModelType.class))
				.computeIfAbsent(sourceType, t -> new HashSet<>())
				.add(sourceId);
	}

	private void putRefId(ModelType type, long id, String refId) {
		refIdToId.put(new TypedRefId(type, refId), id);
		idToRefId.computeIfAbsent(type, t -> new HashMap<>()).put(id, refId);
	}

	private class ModelField {

		private final ModelType type;
		private final String field;
		private final Function<Long, Long> mediator;

		private ModelField(ModelType type, String field) {
			this(type, field, null);
		}

		private ModelField(ModelType type, String field, Function<Long, Long> mediator) {
			this.type = type;
			this.field = field;
			this.mediator = mediator;
		}

	}

	public class ModelReference extends TypedRefId {

		public final long id;

		private ModelReference(ModelType type, long id, String refId) {
			super(type, refId);
			this.id = id;
		}

	}

	private class ReferenceMap extends EnumMap<ModelType, Map<Long, EnumMap<ModelType, Set<Long>>>> {

		private static final long serialVersionUID = 8651176950184800797L;

		public ReferenceMap() {
			super(ModelType.class);
		}

	}

	private interface ResultHandler {

		void handle(long[] ids);

	}

}
