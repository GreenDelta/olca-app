package org.openlca.app.collaboration.navigation;

import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.collaboration.navigation.actions.CommitAction;
import org.openlca.app.collaboration.navigation.actions.ConnectAction;
import org.openlca.app.collaboration.navigation.actions.DiscardAction;
import org.openlca.app.collaboration.navigation.actions.DisconnectAction;
import org.openlca.app.collaboration.navigation.actions.FetchAction;
import org.openlca.app.collaboration.navigation.actions.MergeAction;
import org.openlca.app.collaboration.navigation.actions.OpenCompareViewAction;
import org.openlca.app.collaboration.navigation.actions.PullAction;
import org.openlca.app.collaboration.navigation.actions.PushAction;
import org.openlca.app.collaboration.navigation.actions.ShowCommentsAction;
import org.openlca.app.collaboration.navigation.actions.ShowInHistoryAction;
import org.openlca.app.collaboration.navigation.actions.StashApplyAction;
import org.openlca.app.collaboration.navigation.actions.StashCreateAction;
import org.openlca.app.collaboration.navigation.actions.StashDropAction;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.actions.NavigationMenu;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

public class RepositoryMenu {

	public static void add(List<INavigationElement<?>> selection, IMenuManager menu) {
		if (!isSubOfActiveRepo(selection))
			return;
		var repoMenu = new MenuManager(M.Repository);
		repoMenu.setImageDescriptor(Icon.REPOSITORY.descriptor());
		var added = NavigationMenu.addActions(selection, repoMenu,
				new ConnectAction(),
				new CommitAction(),
				new PushAction(),
				new FetchAction(),
				new MergeAction(),
				new PullAction());
		added += subMenu("Stash", Icon.STASH.descriptor(), selection, repoMenu,
				new StashCreateAction(),
				new StashApplyAction(),
				new StashDropAction());
		added += NavigationMenu.addActions(selection, repoMenu,
				new DiscardAction());
		repoMenu.add(new Separator());
		added += NavigationMenu.addActions(selection, repoMenu,
				new ShowCommentsAction(),
				new ShowInHistoryAction());
		added += subMenu(M.CompareWith, Icon.COMPARE_VIEW.descriptor(), selection, repoMenu,
				new OpenCompareViewAction(true),
				new OpenCompareViewAction(false));
		repoMenu.add(new Separator());
		added += NavigationMenu.addActions(selection, repoMenu,
				new DisconnectAction());
		if (added == 0)
			return;
		menu.add(new Separator());
		menu.add(repoMenu);
		menu.add(new Separator());
	}

	private static int subMenu(String label, ImageDescriptor image, List<INavigationElement<?>> selection,
			MenuManager parentMenu, INavigationAction... actions) {
		var subMenu = new MenuManager(label) {

			@Override
			public boolean isEnabled() {
				for (var action : actions)
					if (action.isEnabled())
						return true;
				return false;
			}

		};
		subMenu.setImageDescriptor(image);
		var added = NavigationMenu.addActions(selection, subMenu, actions);
		if (added == 0)
			return 0;
		parentMenu.add(subMenu);
		return 1;
	}

	private static boolean isSubOfActiveRepo(List<INavigationElement<?>> selection) {
		if (selection.isEmpty())
			return false;
		for (var element : selection)
			if (isSubOfActive(element))
				return true;
		return false;
	}

	private static boolean isSubOfActive(INavigationElement<?> element) {
		while (element != null) {
			if (element instanceof DatabaseElement db) {
				if (Database.isActive(db.getContent()))
					return true;
			}
			element = element.getParent();
		}
		return false;
	}
	
}
