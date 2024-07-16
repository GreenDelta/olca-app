package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

public class StashApplyAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return M.Apply;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.STASH_APPLY.descriptor();
	}

	@Override
	public boolean isEnabled() {
		try {
			var repo = Repository.CURRENT;
			return repo.commits.stash() != null;
		} catch (GitAPIException e) {
			return false;
		}
	}

	@Override
	public void run() {
		try {
			Actions.applyStash();
		} catch (IOException | GitAPIException | InvocationTargetException | InterruptedException e) {
			Actions.handleException("Error applying stashed changes", e);
		} finally {
			Cache.evictAll();
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
