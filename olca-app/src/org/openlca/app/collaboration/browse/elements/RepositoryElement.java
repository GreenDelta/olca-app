package org.openlca.app.collaboration.browse.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.M;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.navigation.elements.Group;
import org.openlca.app.navigation.elements.GroupType;
import org.openlca.collaboration.model.Entry;
import org.openlca.collaboration.model.LibraryInfo;
import org.openlca.collaboration.model.Repository;
import org.openlca.core.model.ModelType;
import org.openlca.git.RepositoryInfo;

public class RepositoryElement extends ServerNavigationElement<Repository> {

	public RepositoryElement(IServerNavigationElement<?> parent, Repository content) {
		super(parent, content);
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	protected List<IServerNavigationElement<?>> queryChildren() {
		var children = new ArrayList<IServerNavigationElement<?>>();
		var counts = WebRequests.execute(
				() -> getClient().browse(getRepositoryId(), ""), new ArrayList<Entry>())
				.stream()
				.collect(Collectors.toMap(Entry::path, Entry::count));
		Arrays.asList(
				ModelType.PROJECT,
				ModelType.PRODUCT_SYSTEM,
				ModelType.PROCESS,
				ModelType.FLOW,
				ModelType.EPD,
				ModelType.RESULT).stream()
				.map(type -> EntryElement.of(this, type, counts.getOrDefault(type.name(), 0)))
				.forEach(children::add);
		children.add(new GroupElement(this, counts, Group.of(M.IndicatorsAndParameters,
				GroupType.INDICATORS,
				ModelType.IMPACT_METHOD,
				ModelType.IMPACT_CATEGORY,
				ModelType.DQ_SYSTEM,
				ModelType.SOCIAL_INDICATOR,
				ModelType.PARAMETER)));
		children.add(new GroupElement(this, counts, Group.of(M.BackgroundData,
				GroupType.BACKGROUND_DATA,
				ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP,
				ModelType.CURRENCY,
				ModelType.ACTOR,
				ModelType.SOURCE,
				ModelType.LOCATION)));
		if (counts.getOrDefault(RepositoryInfo.FILE_NAME, 0) > 0) {
			children.add(new LibrariesElement(this, WebRequests
					.execute(() -> getClient().browse(getRepositoryId(), "LIBRARY"), new ArrayList<Entry>()).stream()
					.map(e -> new LibraryInfo(e.name()))
					.collect(Collectors.toList())));
		}
		return children;
	}

}
