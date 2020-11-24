package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.openlca.app.navigation.elements.INavigationElement;

public interface INavigationAction extends IAction {

	/**
	 * Returns true, if this action accepts the given selection from the
	 * navigation tree. The given selection can be empty or contain one or
	 * multiple elements.
	 */
	boolean accept(List<INavigationElement<?>> selection);

}
