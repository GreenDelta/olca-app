package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitMerge;

public class MergeAction extends Action implements INavigationAction {

	@Override
	public String getText() {
		return "Merge...";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.MERGE.descriptor();
	}

	@Override
	public void run() {
		Database.getWorkspaceIdUpdater().disable();
		var workspaceIds = Repository.get().workspaceIds;
		try {
			var imported = GitMerge
					.from(Repository.get().git)
					.into(Database.get())
					.update(workspaceIds)
					.run();
			if (imported.isEmpty()) {
				MsgBox.info("No changes to merge");
				return;
			}
		} catch (IOException e) {
			Actions.handleException("Error during git merge", e);
		} finally {
			Database.getWorkspaceIdUpdater().enable();
			Cache.evictAll();
			Actions.refresh();
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
