package org.openlca.app.collaboration.navigation.elements;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.NavigationElement;
import org.openlca.app.util.Labels;
import org.openlca.collaboration.model.Entry;
import org.openlca.collaboration.model.TypeOfEntry;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProcessType;
import org.openlca.git.model.ModelRef;

public class EntryElement extends NavigationElement<Entry> implements IRepositoryNavigationElement<Entry> {

	public EntryElement(INavigationElement<?> parent, Entry content) {
		super(parent, content);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		if (getContent().typeOfEntry() == TypeOfEntry.DATASET)
			return new ArrayList<>();
		var children = new ArrayList<INavigationElement<?>>();
		WebRequests.execute(
				() -> getServer().browse(getRepositoryId(), getContent().path()), new ArrayList<Entry>()).stream()
				.map(e -> new EntryElement(this, e))
				.forEach(children::add);
		return children;
	}

	@Override
	public boolean hasChildren() {
		return getContent().count() > 0;
	}

	public ModelType getModelType() {
		if (getContent().typeOfEntry() == TypeOfEntry.MODEL_TYPE)
			return ModelType.parse(getContent().path());
		return new ModelRef(getContent().path()).type;
	}

	public String getRefId() {
		if (getContent().typeOfEntry() != TypeOfEntry.DATASET)
			return null;
		return new ModelRef(getContent().path()).refId;
	}

	public ProcessType getProcessType() {
		var processType = getContent().processType();
		if (processType == null)
			return null;
		return ProcessType.valueOf(processType);
	}

	public FlowType getFlowType() {
		var flowType = getContent().flowType();
		if (flowType == null)
			return null;
		return FlowType.valueOf(flowType);
	}

	public boolean isDataset() {
		return getContent().typeOfEntry() == TypeOfEntry.DATASET;
	}

	public boolean isCategory() {
		return getContent().typeOfEntry() == TypeOfEntry.CATEGORY;
	}

	public boolean isModelType() {
		return getContent().typeOfEntry() == TypeOfEntry.MODEL_TYPE;
	}

	public String getUrl() {
		if (!isDataset())
			return null;
		return getServer().url + "/" + getRepositoryId() + "/dataset/" + getModelType().name() + "/" + getRefId();
	}

	static EntryElement of(INavigationElement<?> parent, ModelType type, int count) {
		return new EntryElement(parent, new Entry(TypeOfEntry.MODEL_TYPE, type.name(), Labels.of(type), count));
	}

}
