package org.openlca.app.collaboration.navigation;

import static org.openlca.core.model.ModelType.ACTOR;
import static org.openlca.core.model.ModelType.CURRENCY;
import static org.openlca.core.model.ModelType.DQ_SYSTEM;
import static org.openlca.core.model.ModelType.EPD;
import static org.openlca.core.model.ModelType.FLOW;
import static org.openlca.core.model.ModelType.FLOW_PROPERTY;
import static org.openlca.core.model.ModelType.IMPACT_CATEGORY;
import static org.openlca.core.model.ModelType.IMPACT_METHOD;
import static org.openlca.core.model.ModelType.LOCATION;
import static org.openlca.core.model.ModelType.PARAMETER;
import static org.openlca.core.model.ModelType.PROCESS;
import static org.openlca.core.model.ModelType.PRODUCT_SYSTEM;
import static org.openlca.core.model.ModelType.PROJECT;
import static org.openlca.core.model.ModelType.RESULT;
import static org.openlca.core.model.ModelType.SOCIAL_INDICATOR;
import static org.openlca.core.model.ModelType.SOURCE;
import static org.openlca.core.model.ModelType.UNIT_GROUP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.navigation.NavElement.ElementType;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Categories;
import org.openlca.util.Categories.PathBuilder;

public class NavRoot {

	private static final ModelType[] UNGROUPED_TYPES = {
			PROJECT, PRODUCT_SYSTEM, PROCESS, FLOW, EPD, RESULT
	};
	private static final ModelType[] GROUP1_TYPES = {
			IMPACT_METHOD, IMPACT_CATEGORY, DQ_SYSTEM, SOCIAL_INDICATOR, PARAMETER
	};
	private static final ModelType[] GROUP2_TYPES = {
			FLOW_PROPERTY, UNIT_GROUP, CURRENCY, ACTOR, SOURCE, LOCATION
	};

	private static NavRoot INSTANCE = new NavRoot(null);
	private Boolean changes;
	private final IDatabase database;
	final PathBuilder categoryPaths;
	private final Map<Long, Category> categoryMap = new HashMap<>();
	private final EnumMap<ModelType, Map<Long, List<Category>>> categories = new EnumMap<>(ModelType.class);
	private final EnumMap<ModelType, Map<Long, List<RootDescriptor>>> descriptors = new EnumMap<>(ModelType.class);
	private final NavElement root = new NavElement(ElementType.DATABASE, null);

	private NavRoot(IDatabase database) {
		this.database = database;
		this.categoryPaths = database != null
				? Categories.pathsOf(database)
				: null;
	}

	public static NavRoot get() {
		return INSTANCE;
	}

	public static void init() {
		var database = Database.get();
		INSTANCE = new NavRoot(database);
		if (database == null || !Repository.isConnected())
			return;
		INSTANCE.build();
	}

	public static void refresh(Runnable navigatorRefresh) {
		navigatorRefresh.run();
		new Thread(() -> {
			init();
			App.runInUI("Refreshing navigator", navigatorRefresh::run);
		}).start();
	}

	static NavElement get(INavigationElement<?> elem) {
		return new NavFinder(NavRoot.get().categoryMap).find(NavRoot.get().root, elem);
	}

	public boolean hasChanges() {
		if (changes == null) {
			changes = RepositoryLabel.hasChanged(Navigator.findElement(Database.getActiveConfiguration()));
		}
		return changes;
	}

	private void build() {
		loadCategories();
		loadDescriptors();
		buildGroup(root, null, UNGROUPED_TYPES);
		buildGroup(root, M.IndicatorsAndParameters, GROUP1_TYPES);
		buildGroup(root, M.BackgroundData, GROUP2_TYPES);
		buildLibraryDir(root);
	}

	private void buildGroup(NavElement parent, String group, ModelType[] types) {
		if (group != null) {
			var root = parent;
			parent = new NavElement(ElementType.GROUP, group);
			root.children().add(parent);
		}
		for (var type : types) {
			parent.children().add(new NavElement(ElementType.MODEL_TYPE, type, buildChildren(type, null)));
		}
	}

	private List<NavElement> buildChildren(ModelType type, Long parentId) {
		var children = new ArrayList<NavElement>();
		children.addAll(build(categories, type, parentId,
				c -> new NavElement(ElementType.CATEGORY, c, buildChildren(type, c.id))));
		children.addAll(build(descriptors, type, parentId,
				d -> new NavElement(ElementType.MODEL, d)));
		return children;
	}

	private void buildLibraryDir(NavElement root) {
		var libs = Database.get().getLibraries();
		if (libs.isEmpty())
			return;
		var libDir = new NavElement(ElementType.LIBRARY_DIR, null);
		for (var lib : libs) {
			libDir.children().add(new NavElement(ElementType.LIBRARY, lib));
		}
		root.children().add(libDir);
	}

	private <T> List<NavElement> build(EnumMap<ModelType, Map<Long, List<T>>> map, ModelType type, Long parentId,
			Function<T, NavElement> builder) {
		return map.getOrDefault(type, Collections.emptyMap())
				.getOrDefault(parentId, Collections.emptyList()).stream()
				.map(builder)
				.toList();
	}

	private void loadCategories() {
		for (var category : new CategoryDao(database).getAll()) {
			if (category.modelType == null)
				continue;
			var parentId = category.category != null ? category.category.id : null;
			put(categories, category.modelType, parentId, category);
			categoryMap.put(category.id, category);
		}
	}

	private void loadDescriptors() {
		for (var type : ModelType.values()) {
			if (type == PARAMETER) {
				for (var descriptor : new ParameterDao(database).getGlobalDescriptors()) {
					put(descriptors, type, descriptor.category, descriptor);
				}
			} else {
				for (var descriptor : database.getDescriptors(type.getModelClass())) {
					put(descriptors, type, descriptor.category, descriptor);
				}
			}
		}
	}

	private <T> void put(EnumMap<ModelType, Map<Long, List<T>>> map, ModelType type, Long parentId, T value) {
		map.computeIfAbsent(type, t -> new HashMap<>())
				.computeIfAbsent(parentId, id -> new ArrayList<>())
				.add(value);
	}

}
