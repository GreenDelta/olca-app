package org.openlca.app.cloud.navigation;

import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.openlca.app.cloud.navigation.action.CommitAction;
import org.openlca.app.cloud.navigation.action.ConnectAction;
import org.openlca.app.cloud.navigation.action.DeleteAction;
import org.openlca.app.cloud.navigation.action.DisconnectAction;
import org.openlca.app.cloud.navigation.action.FetchAction;
import org.openlca.app.cloud.navigation.action.ShareAction;
import org.openlca.app.cloud.navigation.action.UnshareAction;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Viewers;

/**
 * Adds the actions to the context menu of the navigation tree.
 */
public class NavigationActionProvider extends CommonActionProvider {

	private INavigationAction[][] actions = {
			{ new CommitAction(), new FetchAction() },
			{ new ShareAction(), new UnshareAction() },
			{ new ConnectAction(), new DeleteAction(), new DisconnectAction() } };

	@Override
	public void fillContextMenu(IMenuManager menu) {
		ActionContext con = getContext();
		IStructuredSelection selection = (IStructuredSelection) con
				.getSelection();
		List<INavigationElement<?>> elements = Viewers.getAll(selection);
		if (elements.size() == 1)
			registerSingleActions(elements.get(0), menu);
		else if (elements.size() > 1)
			registerMultiActions(elements, menu);
	}

	private int registerSingleActions(INavigationElement<?> element,
			IMenuManager menu) {
		int count = 0;
		for (INavigationAction[] group : actions) {
			boolean acceptedOne = false;
			for (INavigationAction action : group)
				if (action.accept(element)) {
					menu.add(action);
					count++;
					acceptedOne = true;
				}
			if (acceptedOne)
				menu.add(new Separator());
		}
		return count;
	}

	private int registerMultiActions(List<INavigationElement<?>> elements,
			IMenuManager menu) {
		int count = 0;
		for (INavigationAction[] group : actions) {
			boolean acceptedOne = false;
			for (INavigationAction action : group)
				if (action.accept(elements)) {
					menu.add(action);
					count++;
					acceptedOne = true;
				}
			if (acceptedOne)
				menu.add(new Separator());
		}
		return count;
	}

}
