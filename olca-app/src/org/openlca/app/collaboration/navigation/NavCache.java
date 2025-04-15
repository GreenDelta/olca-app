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
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.navigation.NavElement.ElementType;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

public class NavCache {

	private static final ModelType[] UNGROUPED_TYPES = {
			PROJECT, PRODUCT_SYSTEM, PROCESS, FLOW, EPD, RESULT
	};
	private static final ModelType[] GROUP1_TYPES = {
			IMPACT_METHOD, IMPACT_CATEGORY, DQ_SYSTEM, SOCIAL_INDICATOR, PARAMETER
	};
	private static final ModelType[] GROUP2_TYPES = {
			FLOW_PROPERTY, UNIT_GROUP, CURRENCY, ACTOR, SOURCE, LOCATION
	};

	private static NavCache INSTANCE = new NavCache();
	private final NavElement root = new NavElement(ElementType.DATABASE, null);
	private Boolean changes;

	private NavCache() {
	}

	public static NavCache get() {
		return INSTANCE;
	}

	public static void refresh() {
		refresh(null);
	}

	public static void refresh(ModelType type) {
		var database = Database.get();
		INSTANCE = new NavCache();
		if (database == null || Repository.get() == null)
			return;
		if (type == null) {
			Repository.descriptors().reload();
		} else {
			Repository.descriptors().reload(type);
		}
		INSTANCE.build();
	}

	static NavElement get(INavigationElement<?> elem) {
		return new NavFinder(Repository.get()).find(INSTANCE.root, elem);
	}

	public boolean hasChanges() {
		if (changes == null) {
			changes = RepositoryLabel.hasChanged(Navigator.findElement(Database.getActiveConfiguration()));
		}
		return changes;
	}

	private void build() {
		buildGroup(root, null, UNGROUPED_TYPES);
		buildGroup(root, M.IndicatorsAndParameters, GROUP1_TYPES);
		buildGroup(root, M.BackgroundData, GROUP2_TYPES);
		buildDataPackages(root);
	}

	private void buildGroup(NavElement parent, String group, ModelType[] types) {
		if (group != null) {
			var root = parent;
			parent = new NavElement(ElementType.GROUP, group);
			root.children().add(parent);
		}
		for (var type : types) {
			parent.children().add(new NavElement(ElementType.MODEL_TYPE, type, false, buildChildren(type, null)));
		}
	}

	private List<NavElement> buildChildren(ModelType type, Category category) {
		var children = new ArrayList<NavElement>();
		children.addAll(buildCategories(type, category));
		children.addAll(buildDatasets(type, category));
		return children;
	}

	private void buildDataPackages(NavElement root) {
		var packages = Database.dataPackages();
		if (packages.isEmpty())
			return;
		var packagesElem = new NavElement(ElementType.DATAPACKAGES);
		for (var p : packages.getAll()) {
			packagesElem.children().add(new NavElement(ElementType.DATAPACKAGE, p));
		}
		root.children().add(packagesElem);
	}

	private List<NavElement> buildCategories(ModelType type, Category category) {
		var categories = category != null
				? category.childCategories
				: Repository.descriptors().getCategories(type);
		return categories.stream()
				.map(c -> {
					var children = buildChildren(type, c);
					return new NavElement(ElementType.CATEGORY, c, isOnlyDataPackage(children), children);
				})
				.collect(Collectors.toList());
	}

	private boolean isOnlyDataPackage(List<NavElement> elements) {
		if (elements.isEmpty())
			return false;
		for (var element : elements)
			if (!element.isFromDataPackage())
				return false;
		return true;
	}

	private List<NavElement> buildDatasets(ModelType type, Category category) {
		var datasets = category != null
				? Repository.descriptors().get(category)
				: Repository.descriptors().get(type);
		return datasets.stream()
				.map(NavElement::new)
				.collect(Collectors.toList());
	}

}
