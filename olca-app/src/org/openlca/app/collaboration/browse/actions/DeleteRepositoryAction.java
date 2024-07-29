package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.collaboration.browse.ServerNavigator;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.browse.elements.RepositoryElement;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Question;

class DeleteRepositoryAction extends Action implements IServerNavigationAction {

	private RepositoryElement elem;

	DeleteRepositoryAction() {
		setText(M.Delete);
		setImageDescriptor(Icon.DELETE.descriptor());
	}

	@Override
	public void run() {
		if (!Question.askDelete(elem.getRepositoryId()))
			return;
		if (!WebRequests.execute(() -> elem.getClient().deleteRepository(elem.getRepositoryId())))
			return;
		ServerNavigator.refresh();
	}

	@Override
	public boolean accept(List<IServerNavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof RepositoryElement repoElem))
			return false;
		this.elem = repoElem;
		return true;
	}

}