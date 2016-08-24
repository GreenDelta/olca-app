package org.openlca.app.editors.graphical.model;

import java.util.List;

import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

abstract class AppAbstractEditPart<N extends Node> extends AbstractGraphicalEditPart {

	@Override
	public void activate() {
		getModel().editPart = this;
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
