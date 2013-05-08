/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.navigation;

import org.openlca.core.model.modelprovider.IModelComponent;

/**
 * Extension of the {@link AbstractNavigationElement} for a model component in
 * the navigator
 * 
 * @author Sebastian Greve
 * 
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
