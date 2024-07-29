package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;

interface IServerNavigationAction extends IAction {

	/**
	 * Returns true, if this action accepts the given selection from the
	 * navigation tree. The given selection can be empty or contain one or
	 * multiple elements.
	 */
	boolean accept(List<IServerNavigationElement<?>> selection);

}
