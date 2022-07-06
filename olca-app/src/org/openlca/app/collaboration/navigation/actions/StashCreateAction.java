package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.navigation.RepositoryLabel;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.git.actions.GitStashCreate;
import org.openlca.git.model.Change;

public class StashCreateAction extends Action implements INavigationAction {

	private List<INavigationElement<?>> selection;

	@Override
	public String getText() {
		return "Create";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.STASH_CREATE.descriptor();
	}

	@Override
	public boolean isEnabled() {
		try {
			var repo = Repository.get();
			if (!RepositoryLabel.hasChanged(Navigator.findElement(Database.getActiveConfiguration())))
				return false;
			return Actions.getStashCommit(repo.git) == null;
		} catch (GitAPIException e) {
			return false;
		}
	}

	@Override
	public void run() {
		Database.getWorkspaceIdUpdater().disable();
		var repo = Repository.get();
		try {
			var input = Datasets.select(selection, false, true);
			if (input == null)
				return;
			var user = AuthenticationDialog.promptUser(repo);
			if (user == null)
				return;
			var changes = input.datasets().stream()
					.map(d -> new Change(d.leftDiffType, d))
					.collect(Collectors.toList());
			Actions.run(GitStashCreate.from(Database.get())
					.to(repo.git)
					.as(user)
					.changes(changes)
					.update(repo.workspaceIds));
		} catch (IOException | InvocationTargetException | InterruptedException | GitAPIException e) {
			Actions.handleException("Error stashing changes", e);
		} finally {
			Database.getWorkspaceIdUpdater().enable();
			Cache.evictAll();
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (!Repository.isConnected())
			return false;
		this.selection = selection;
		return true;
	}

}
