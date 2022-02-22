package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.collaboration.dialogs.HistoryDialog;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitFetch;
import org.openlca.git.actions.GitMerge;

public class PullAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return "Pull...";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.PULL.descriptor();
	}

	@Override
	public void run() {
		try {
			var newCommits = Actions.run(GitFetch
					.from(Repository.get().git)
					.authorizeWith(Actions.credentialsProvider()));
			if (!newCommits.isEmpty()) {
				new HistoryDialog("Fetched commits", newCommits).open();
			}
			var imported = GitMerge
					.from(Repository.get().git)
					.into(Database.get())
					.update(Repository.get().workspaceIds)
					.run();
			if (imported.isEmpty()) {
				MsgBox.info("No commits to fetch - Everything up to date");
			}
		} catch (IOException | InvocationTargetException | InterruptedException | GitAPIException e) {
			Actions.handleException("Error pulling data", e);
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
