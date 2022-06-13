package org.openlca.app.editors.graphical_legacy.model;

import java.util.List;

import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

/**
 * An abstract part that will handle events management.
 * @param <N>
 *   Type of the model element linked by the extended {@link
 *   org.eclipse.gef.EditPart}.
 */
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
