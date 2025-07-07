package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.git.util.Constants;

class MergeAction extends Action implements INavigationAction {

	private Repository repo;
	
	@Override
	public String getText() {
		return M.MergeDots;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.MERGE.descriptor();
	}

	@Override
	public boolean isEnabled() {
		return !repo.localHistory.getBehindOf(Constants.REMOTE_REF).isEmpty();
	}

	@Override
	public void run() {
		try {
			Merge.on(repo, null, false);
		} catch (IOException | GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error during Git merge", e);
		} finally {
			AppContext.evictAll();
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		repo = Actions.getRepo(selection);
		return repo != null;
	}

}
