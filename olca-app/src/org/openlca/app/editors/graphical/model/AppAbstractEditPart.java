/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

abstract class AppAbstractEditPart<N extends Node> extends
		AbstractGraphicalEditPart {

	@Override
	public void activate() {
		getModel().setEditPart(this);
		super.activate();
	}

	@Override
	public void refreshChildren() {
		super.refreshChildren(); // make visible
	}

	@SuppressWarnings("unchecked")
	@Override
	public N getModel() {
		return (N) super.getModel();
	}

	@Override
	protected final List<? extends Node> getModelChildren() {
		return getModel().getChildren();
	}

}
