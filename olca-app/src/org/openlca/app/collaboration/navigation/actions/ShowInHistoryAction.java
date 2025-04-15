package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

class ShowInHistoryAction extends Action implements INavigationAction {

	private Repository repo;

	ShowInHistoryAction() {
		setText(M.ShowInHistory);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.HISTORY_VIEW.descriptor();
	}

	@Override
	public void run() {
		HistoryView.update(repo);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		repo = Actions.getRepo(selection);
		return repo != null;
	}

}
