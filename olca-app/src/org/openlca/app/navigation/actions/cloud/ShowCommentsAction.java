package org.openlca.app.navigation.actions.cloud;

import org.openlca.app.M;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.db.Database;
import org.openlca.app.editors.CommentsEditor;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;

public class ShowCommentsAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.ShowComments;
	}

	@Override
	public void run() {
		CommentsEditor.open();
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var client = Database.getRepositoryClient();
		return client != null;
	}
}
