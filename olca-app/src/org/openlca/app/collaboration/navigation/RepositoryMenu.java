package org.openlca.app.collaboration.navigation;

import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.openlca.app.M;
import org.openlca.app.collaboration.navigation.actions.CommitAction;
import org.openlca.app.collaboration.navigation.actions.ConnectAction;
import org.openlca.app.collaboration.navigation.actions.DisconnectAction;
import org.openlca.app.collaboration.navigation.actions.FetchAction;
import org.openlca.app.collaboration.navigation.actions.MergeAction;
import org.openlca.app.collaboration.navigation.actions.OpenCompareViewAction;
import org.openlca.app.collaboration.navigation.actions.PullAction;
import org.openlca.app.collaboration.navigation.actions.PushAction;
import org.openlca.app.collaboration.navigation.actions.ShowCommentsAction;
import org.openlca.app.collaboration.navigation.actions.ShowInHistoryAction;
import org.openlca.app.navigation.actions.NavigationMenu;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

public class RepositoryMenu {

	public static void add(List<INavigationElement<?>> selection, IMenuManager menu) {
		var subMenu = new MenuManager(M.Repository);
		subMenu.setImageDescriptor(Icon.REPOSITORY.descriptor());
		var added = NavigationMenu.addActions(selection, subMenu,
				new ConnectAction(),
				new CommitAction(),
				new PushAction(),
				new FetchAction(),
				new PullAction(),
				new MergeAction());
		added += NavigationMenu.addActions(selection, subMenu,
				new ShowCommentsAction(),
				new ShowInHistoryAction());
		// compare sub menu
		var compareMenu = new MenuManager(M.CompareWith);
		compareMenu.setImageDescriptor(Icon.COMPARE_VIEW.descriptor());
		var subAdded = NavigationMenu.addActions(selection, compareMenu,
				new OpenCompareViewAction(true),
				new OpenCompareViewAction(false));
		if (subAdded > 0) {
			subMenu.add(compareMenu);
			subMenu.add(new Separator());
			added++;
		}
		added += NavigationMenu.addActions(selection, subMenu,
				new DisconnectAction());
		if (added == 0)
			return;
		menu.add(new Separator());
		menu.add(subMenu);
		menu.add(new Separator());
	}

}
