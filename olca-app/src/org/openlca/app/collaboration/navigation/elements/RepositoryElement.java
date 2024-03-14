package org.openlca.app.collaboration.navigation.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.M;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.navigation.elements.Group;
import org.openlca.app.navigation.elements.GroupType;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.NavigationElement;
import org.openlca.collaboration.model.Entry;
import org.openlca.collaboration.model.Repository;
import org.openlca.core.model.ModelType;

public class RepositoryElement extends NavigationElement<Repository>
		implements IRepositoryNavigationElement<Repository> {

	public RepositoryElement(INavigationElement<?> parent, Repository content) {
		super(parent, content);
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var children = new ArrayList<INavigationElement<?>>();
		var counts = WebRequests.execute(
				() -> getServer().browse(getRepositoryId(), "").stream()
						.collect(Collectors.toMap(Entry::path, Entry::count)));
		Arrays.asList(
				ModelType.PROJECT,
				ModelType.PRODUCT_SYSTEM,
				ModelType.PROCESS,
				ModelType.FLOW,
				ModelType.EPD,
				ModelType.RESULT).stream()
				.map(type -> EntryElement.of(this, type, counts.getOrDefault(type.name(), 0)))
				.forEach(children::add);
		children.add(new RepositoryGroupElement(this, counts, Group.of(M.IndicatorsAndParameters,
				GroupType.INDICATORS,
				ModelType.IMPACT_METHOD,
				ModelType.IMPACT_CATEGORY,
				ModelType.DQ_SYSTEM,
				ModelType.SOCIAL_INDICATOR,
				ModelType.PARAMETER)));
		children.add(new RepositoryGroupElement(this, counts, Group.of(M.BackgroundData,
				GroupType.BACKGROUND_DATA,
				ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP,
				ModelType.CURRENCY,
				ModelType.ACTOR,
				ModelType.SOURCE,
				ModelType.LOCATION)));
		// addLibraryElements(list);
		return children;
	}

}
