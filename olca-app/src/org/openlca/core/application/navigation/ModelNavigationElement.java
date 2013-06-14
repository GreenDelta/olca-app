package org.openlca.core.application.navigation;

import org.openlca.core.model.modelprovider.IModelComponent;

/**
 * Content of this navigation element is a reference to a model (Process, Flow,
 * Unit group etc.).
 */
public class ModelNavigationElement extends AbstractNavigationElement {

	private IModelComponent modelComponent;
	private INavigationElement parent;

	public ModelNavigationElement(INavigationElement parent,
			IModelComponent modelComponent) {
		this.modelComponent = modelComponent;
		this.parent = parent;
	}

	@Override
	protected void refresh() {
	}

	@Override
	public Object getData() {
		return modelComponent;
	}

	@Override
	public INavigationElement getParent() {
		return parent;
	}

	@Override
	public String toString() {
		String str = "ModelNavigationElement [ modelComponent = ";
		if (modelComponent == null)
			str += "null ]";
		else
			str += "(" + modelComponent.getName() + ", "
					+ modelComponent.getClass() + ")]";
		return str;
	}

}
