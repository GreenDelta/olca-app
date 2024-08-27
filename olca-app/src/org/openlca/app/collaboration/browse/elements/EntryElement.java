package org.openlca.app.collaboration.browse.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.util.Labels;
import org.openlca.collaboration.model.Entry;
import org.openlca.collaboration.model.TypeOfEntry;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProcessType;
import org.openlca.git.model.ModelRef;

public class EntryElement extends ServerNavigationElement<Entry> {

	public EntryElement(IServerNavigationElement<?> parent, Entry content) {
		super(parent, content);
	}

	@Override
	protected List<IServerNavigationElement<?>> queryChildren() {
		if (getContent().typeOfEntry() == TypeOfEntry.DATASET)
			return new ArrayList<>();
		return WebRequests.execute(
				() -> getClient().browse(getRepositoryId(), getContent().path()), new ArrayList<Entry>()).stream()
				.map(e -> new EntryElement(this, e))
				.collect(Collectors.toList());
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
		return getClient().url + "/" + getRepositoryId() + "/dataset/" + getModelType().name() + "/" + getRefId();
	}

	static EntryElement of(IServerNavigationElement<?> parent, ModelType type, int count) {
		return new EntryElement(parent, new Entry(TypeOfEntry.MODEL_TYPE, type.name(), Labels.of(type), count));
	}

}
