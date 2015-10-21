package org.openlca.app.cloud.navigation.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.openlca.app.App;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffIndexer;
import org.openlca.app.cloud.navigation.RepositoryElement;
import org.openlca.app.cloud.navigation.RepositoryNavigator;
import org.openlca.app.cloud.ui.CommitDialog;
import org.openlca.app.cloud.ui.DiffNode;
import org.openlca.app.cloud.ui.DiffNodeBuilder;
import org.openlca.app.cloud.ui.DiffResult;
import org.openlca.app.cloud.ui.DiffResult.DiffResponse;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;
import org.openlca.cloud.api.CommitInvocation;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.util.WebRequests.WebRequestException;

public class CommitAction extends Action implements INavigationAction {

	private DiffIndex index;
	private RepositoryClient client;

	public CommitAction() {
		setText("#Commit...");
	}

	@Override
	public void run() {
		Runner runner = new Runner();
		runner.run();
		if (runner.error != null)
			Error.showBox(runner.error.getMessage());
		if (!runner.upToDate)
			Error.showBox("#Rejected - not up to date. Please fetch the latest changes from the repository first");
		if (runner.noChanges)
			Info.showBox("#No changes in local db");
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		while (!(element instanceof RepositoryElement))
			element = element.getParent();
		client = RepositoryNavigator.getClient();
		if (client == null)
			return false;
		index = RepositoryNavigator.getDiffIndex();
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return accept(elements.get(0));
	}

	private class Runner {

		private boolean upToDate = true;
		private boolean noChanges = false;
		private String message;
		private List<DiffResult> changes;
		private WebRequestException error;

		public void run() {
			App.runWithProgress("#Comparing with repository",
					this::getDifferences);
			openCommitDialog();
		}

		private void getDifferences() {
			checkUpToDate(client);
			if (!canContinue())
				return;
			changes = createDifferences(index, index.getChanged());
		}

		private void openCommitDialog() {
			if (!canContinue())
				return;
			DiffNode node = new DiffNodeBuilder(Database.get(),
					RepositoryNavigator.getDiffIndex()).build(changes);
			if (node == null) {
				noChanges = true;
				return;
			}
			CommitDialog dialog = new CommitDialog(node, client);
			dialog.setBlockOnOpen(true);
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			message = dialog.getMessage();
			App.runWithProgress("#Commiting changes", this::commit);
			afterCommit();
		}

		private void commit() {
			try {
				checkUpToDate(client);
				if (!canContinue())
					return;
				CommitInvocation commit = client.createCommitInvocation();
				commit.setCommitMessage(message);
				putChanges(changes, commit);
				client.execute(commit);
				DiffIndexer indexer = new DiffIndexer(index);
				indexer.indexCommit(CloudUtil.toDescriptors(changes));
			} catch (WebRequestException e) {
				error = e;
			}
		}

		private void afterCommit() {
			if (!canContinue())
				return;
			RepositoryNavigator.refresh();
		}

		private List<DiffResult> createDifferences(DiffIndex index,
				List<Diff> changes) {
			List<DiffResult> differences = new ArrayList<>();
			for (Diff diff : changes)
				differences.add(new DiffResult(diff));
			return differences;
		}

		private void checkUpToDate(RepositoryClient client) {
			try {
				upToDate = client.requestCommit();
			} catch (WebRequestException e) {
				error = e;
			}
		}

		private boolean canContinue() {
			if (error != null)
				return false;
			if (!upToDate)
				return false;
			return true;
		}

		private void putChanges(List<DiffResult> changes,
				CommitInvocation commit) {
			for (DiffResult change : changes)
				if (change.getType() == DiffResponse.DELETE_FROM_REMOTE)
					commit.put(change.getDescriptor(), null);
				else
					commit.put(Database.createRootDao(
							change.getDescriptor().getType()).getForRefId(
							change.getDescriptor().getRefId()));
		}

	}

}
