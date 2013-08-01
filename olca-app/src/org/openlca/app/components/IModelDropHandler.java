package org.openlca.app.components;

import java.util.List;

import org.openlca.core.model.descriptors.BaseDescriptor;

public interface IModelDropHandler {

	void handleDrop(List<BaseDescriptor> droppedModels);

}
