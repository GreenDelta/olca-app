package org.openlca.core.application.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.openlca.core.application.navigation.INavigationElement;

public interface INavigationAction extends IAction {

	boolean accept(INavigationElement element);

	boolean accept(List<INavigationElement> elements);

}
