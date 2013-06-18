package org.openlca.core.application.navigation;

import java.util.Collections;
import java.util.List;

import org.openlca.core.model.ModelType;

public class ModelTypeElement implements INavigationElement {

	private ModelType modelType;

	public ModelTypeElement(ModelType modelType) {
		this.modelType = modelType;
	}

	@Override
	public List<INavigationElement> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public INavigationElement getParent() {
		return null;
	}

	@Override
	public Object getData() {
		return modelType;
	}

}
