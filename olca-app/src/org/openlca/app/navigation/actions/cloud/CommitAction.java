package org.openlca.app.navigation.actions.cloud;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.openlca.app.App;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.ui.CommitDialog;
import org.openlca.app.cloud.ui.diff.DiffNode;
import org.openlca.app.cloud.ui.diff.DiffNodeBuilder;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;
import org.openlca.cloud.api.CommitInvocation;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.model.ModelType;

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
		while (!(element instanceof DatabaseElement))
			element = element.getParent();
		client = Database.getRepositoryClient();
		if (client == null)
			return false;
		index = Database.getDiffIndex();
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
					Database.getDiffIndex()).build(changes);
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
				indexCommit();
			} catch (WebRequestException e) {
				error = e;
			}
		}

		private void indexCommit() {
			for (DiffResult change : changes) {
				Dataset dataset = change.getDataset();
				DiffType before = index.get(dataset.getRefId()).type;
				if (before == DiffType.DELETED)
					index.remove(dataset.getRefId());
				else
					index.update(dataset, DiffType.NO_DIFF);
			}
			index.commit();
		}

		private void afterCommit() {
			if (!canContinue())
				return;
			Navigator.refresh(Navigator.getNavigationRoot());
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
				if (change.getType() == DiffResponse.DELETE_FROM_REMOTE) {
					Dataset dataset = change.getDataset();
					dataset.setFullPath(change.local.dataset.getFullPath());
					commit.putForRemoval(dataset);
				} else {
					ModelType type = change.getDataset().getType();
					String refId = change.getDataset().getRefId();
					commit.put(Database.createRootDao(type).getForRefId(refId));
				}
		}
	}

}
