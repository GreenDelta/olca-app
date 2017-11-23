package org.openlca.app.navigation.actions.cloud;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.db.Database;
import org.openlca.app.editors.CommentsEditor;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.cloud.api.RepositoryClient;

public class ShowCommentsAction extends Action implements INavigationAction {

	private RepositoryClient client;

	@Override
	public String getText() {
		return "#Show comments";
	}

	@Override
	public void run() {
		CommentsEditor.open();
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		client = Database.getRepositoryClient();
		if (client == null)
			return false;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
