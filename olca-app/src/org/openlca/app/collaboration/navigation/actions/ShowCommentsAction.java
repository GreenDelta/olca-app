package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.editors.CommentsEditor;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

class ShowCommentsAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.ShowComments;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.COMMENTS_VIEW.descriptor();
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
		if (!CollaborationPreference.commentsEnabled())
			return false;
		var repo = Actions.getRepo(selection);
		return repo != null && repo.dataPackage == null && repo.isCollaborationServer();
	}
}
