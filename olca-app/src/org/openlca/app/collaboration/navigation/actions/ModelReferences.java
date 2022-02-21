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

class ModelReferences {

	private IDatabase database;
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

	public Set<ModelReference> get(ModelType type, Set<Long> ids) {
		var refs = new HashSet<ModelReference>();
		refs.addAll(getReferences(type, ids));
		refs.addAll(getUsages(type, ids));
		return refs;
	}

	public Set<ModelReference> getReferences(ModelType type, Set<Long> ids) {
		return get(references, type, ids);
	}

	public Set<ModelReference> getUsages(ModelType type, Set<Long> ids) {
		return get(usages, type, ids);
	}

	private Set<ModelReference> get(ReferenceMap map, ModelType type, Set<Long> ids) {
		var refs = new HashSet<ModelReference>();
		var typeMap = map.get(type);
		if (typeMap == null)
			return refs;
		for (var id : ids) {
			var idMap = typeMap.get(id);
			if (idMap == null)
				continue;
			for (var targetType : idMap.keySet()) {
				for (var targetId : idMap.get(targetType)) {
					refs.add(new ModelReference(targetType, targetId));
				}
			}
		}
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
		scanCurrencies();
		scanUnitGroups();
		scanFlowProperties();
		scanSocialIndicators();
		scanImpactCategories();
		scanImpactMethods();
		scanFlows();
		scanProcesses();
		scanProductSystems();
		scanProjects();
	}

	private void scanCurrencies() {
		scanTable("tbl_currencies",
				new ModelField(ModelType.CURRENCY, "id"),
				new ModelField(ModelType.CURRENCY, "f_reference_currency"));
	}

	private void scanUnitGroups() {
		scanTable("tbl_unit_groups",
				new ModelField(ModelType.UNIT_GROUP, "id"),
				new ModelField(ModelType.FLOW_PROPERTY, "f_default_flow_property"));
	}

	private void scanFlowProperties() {
		scanTable("tbl_flow_properties",
				new ModelField(ModelType.FLOW_PROPERTY, "id"),
				new ModelField(ModelType.UNIT_GROUP, "f_unit_group"));
	}

	private void scanSocialIndicators() {
		scanTable("tbl_social_indicators",
				new ModelField(ModelType.SOCIAL_INDICATOR, "id"),
				new ModelField(ModelType.FLOW_PROPERTY, "f_activity_quantity"));
	}

	private void scanImpactCategories() {
		scanTable("tbl_impact_categories",
				new ModelField(ModelType.IMPACT_METHOD, "id"),
				new ModelField(ModelType.SOURCE, "f_source"));
		scanTable("tbl_impact_factors",
				new ModelField(ModelType.IMPACT_CATEGORY, "f_impact_category"),
				new ModelField(ModelType.FLOW, "f_flow"),
				new ModelField(ModelType.LOCATION, "f_location"));
	}

	private void scanImpactMethods() {
		scanTable("tbl_impact_methods",
				new ModelField(ModelType.IMPACT_METHOD, "id"),
				new ModelField(ModelType.ACTOR, "f_author"),
				new ModelField(ModelType.ACTOR, "f_generator"),
				new ModelField(ModelType.SOURCE, "f_source"));
		scanTable("tbl_source_links",
				new ModelField(ModelType.IMPACT_METHOD, "f_owner"),
				new ModelField(ModelType.SOURCE, "f_source"));
		scanTable("tbl_impact_links",
				new ModelField(ModelType.IMPACT_METHOD, "f_impact_method"),
				new ModelField(ModelType.IMPACT_CATEGORY, "f_impact_category"));
	}

	private void scanFlows() {
		scanTable("tbl_flow_property_factors",
				new ModelField(ModelType.FLOW, "f_flow"),
				new ModelField(ModelType.FLOW_PROPERTY, "f_flow_property"));
	}

	private void scanProcesses() {
		var docsToProcess = scanTable("tbl_processes", "f_process_doc",
				new ModelField(ModelType.PROCESS, "id"),
				new ModelField(ModelType.LOCATION, "f_location"),
				new ModelField(ModelType.DQ_SYSTEM, "f_dq_system"),
				new ModelField(ModelType.DQ_SYSTEM, "f_exchange_dq_system"),
				new ModelField(ModelType.DQ_SYSTEM, "f_social_dq_system"));
		scanTable("tbl_process_docs",
				new ModelField(ModelType.PROCESS, "id", docsToProcess::get),
				new ModelField(ModelType.ACTOR, "f_reviewer"),
				new ModelField(ModelType.ACTOR, "f_data_documentor"),
				new ModelField(ModelType.ACTOR, "f_data_generator"),
				new ModelField(ModelType.ACTOR, "f_dataset_owner"),
				new ModelField(ModelType.SOURCE, "f_publication"));
		scanTable("tbl_source_links",
				new ModelField(ModelType.PROCESS, "f_owner", docsToProcess::get),
				new ModelField(ModelType.SOURCE, "f_source"));
		scanTable("tbl_exchanges",
				new ModelField(ModelType.PROCESS, "f_owner"),
				new ModelField(ModelType.PROCESS, "f_default_provider"),
				new ModelField(ModelType.FLOW, "f_flow"),
				new ModelField(ModelType.FLOW, "f_location"),
				new ModelField(ModelType.FLOW, "f_currency"));
	}

	private void scanProductSystems() {
		scanTable("tbl_product_systems",
				new ModelField(ModelType.PRODUCT_SYSTEM, "id"),
				new ModelField(ModelType.PROCESS, "f_reference_process"));
		scanTable("tbl_process_links",
				new ModelField(ModelType.PRODUCT_SYSTEM, "f_product_system"),
				new ModelField(ModelType.PROCESS, "f_process"),
				new ModelField(ModelType.PROCESS, "f_provider"));
		var setToSystem = scanTable("tbl_parameter_redef_sets", "id",
				new ModelField(ModelType.PRODUCT_SYSTEM, "f_product_system"));
		scanParameterRedefs(ModelType.PRODUCT_SYSTEM, setToSystem::get);
	}

	private void scanProjects() {
		scanTable("tbl_projects",
				new ModelField(ModelType.PROJECT, "id"),
				new ModelField(ModelType.IMPACT_METHOD, "f_impact_method"));
		var variantToProject = scanTable("tbl_project_variants", "id",
				new ModelField(ModelType.PROJECT, "f_project"),
				new ModelField(ModelType.PRODUCT_SYSTEM, "f_product_system"));
		scanParameterRedefs(ModelType.PROJECT, variantToProject::get);
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
			put(ownerType, ownerId, ModelType.PARAMETER, parameterId);
			return true;
		});
	}

	private void scanTable(String table, ModelField source, ModelField... targets) {
		scanTable(table, null, source, targets);
	}

	/**
	 * if idField is not null, idField is queried additionally and a map between
	 * the value of source.field and value of idField is returned, otherwise an
	 * empty map
	 */
	private Map<Long, Long> scanTable(String table, String idField, ModelField source, ModelField... targets) {
		var targetFields = Arrays.stream(targets).map(t -> t.field).toArray(n -> new String[n]);
		var map = new HashMap<Long, Long>();
		query(table, source.field, idField, targetFields, ids -> {
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
				put(source.type, sourceId, target.type, targetId);
			}
		});
		return map;
	}

	private void put(ModelType sourceType, long sourceId, ModelType targetType, long targetId) {
		references.computeIfAbsent(sourceType, t -> new HashMap<>())
				.computeIfAbsent(sourceId, t -> new EnumMap<>(ModelType.class))
				.computeIfAbsent(targetType, t -> new HashSet<>())
				.add(targetId);
		usages.computeIfAbsent(targetType, t -> new HashMap<>())
				.computeIfAbsent(targetId, t -> new EnumMap<>(ModelType.class))
				.computeIfAbsent(sourceType, t -> new HashSet<>())
				.add(sourceId);
	}

	private void query(String table, String sourceField, String idField, String[] targets, ResultHandler handler) {
		var fields = new ArrayList<String>();
		fields.add(sourceField);
		if (idField != null) {
			fields.add(idField);
		}
		fields.addAll(Arrays.asList(targets));
		var query = "SELECT " + fields.stream().collect(Collectors.joining(",")) + " FROM " + table;
		NativeSql.on(database).query(query, rs -> {
			var ids = new long[fields.size()];
			for (var i = 0; i < fields.size(); i++) {
				ids[i] += rs.getLong(i + 1);
			}
			handler.handle(ids);
			return true;
		});
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

	public record ModelReference(ModelType type, long id) {
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
