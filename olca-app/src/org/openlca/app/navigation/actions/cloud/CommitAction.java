package org.openlca.app.navigation.actions.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.openlca.app.cloud.ui.library.LibraryResultDialog;
import org.openlca.app.cloud.ui.preferences.CloudPreference;
import org.openlca.app.db.Database;
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
	private List<INavigationElement<?>> selection;

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
		if (!Database.isConnected())
			return false;
		index = Database.getDiffIndex();
		client = Database.getRepositoryClient();
		selection = Collections.singletonList(element);
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		if (!Database.isConnected())
			return false;
		index = Database.getDiffIndex();
		client = Database.getRepositoryClient();
		selection = elements;
		return true;
	}

	private class Runner {

		private boolean upToDate = true;
		private boolean noChanges = false;
		private boolean canceled = false;
		private String message;
		private List<DiffResult> changes;
		private List<DiffResult> selected;
		private Map<Dataset, String> checkResult;
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
			Set<String> refIds = new RefIdListBuilder(selection, changes, index)
					.build();
			dialog.setInitialSelection(refIds);
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			message = dialog.getMessage();
			selected = dialog.getSelected();
			doLibraryCheck();
			if (canceled)
				return;
			App.runWithProgress("#Commiting changes", this::commit);
			afterCommit();
		}

		private void doLibraryCheck() {
			if (!CloudPreference.doCheckAgainstLibraries())
				return;
			App.runWithProgress("#Checking against libraries",
					this::checkAgainstLibraries);
			if (!canContinue())
				return;
			if (checkResult == null || checkResult.isEmpty())
				return;
			LibraryResultDialog libraryDialog = new LibraryResultDialog(
					checkResult);
			if (libraryDialog.open() != IDialogConstants.OK_ID)
				canceled = true;
		}

		private void checkAgainstLibraries() {
			Set<Dataset> datasets = new HashSet<>();
			for (DiffResult result : selected)
				datasets.add(result.getDataset());
			try {
				checkResult = client.performLibraryCheck(datasets);
			} catch (WebRequestException e) {
				error = e;
			}
		}

		private void commit() {
			try {
				checkUpToDate(client);
				if (!canContinue())
					return;
				CommitInvocation commit = client.createCommitInvocation();
				commit.setCommitMessage(message);
				putChanges(selected, commit);
				client.execute(commit);
				indexCommit();
			} catch (WebRequestException e) {
				error = e;
			}
		}

		private void indexCommit() {
			orderResults();
			for (DiffResult change : selected) {
				Dataset dataset = change.getDataset();
				DiffType before = index.get(dataset.getRefId()).type;
				if (before == DiffType.DELETED)
					index.remove(dataset.getRefId());
				else
					index.update(dataset, DiffType.NO_DIFF);
			}
			index.commit();
		}

		// must order diffs by length of path, so when removing from index,
		// parent updating always works
		private void orderResults() {
			Collections.sort(selected, (d1, d2) -> {
				int depth1 = 0;
				String path = d1.getDataset().getFullPath();
				while (path.contains("/")) {
					path = path.substring(path.indexOf("/") + 1);
					depth1++;
				}
				int depth2 = 0;
				path = d2.getDataset().getFullPath();
				while (path.contains("/")) {
					path = path.substring(path.indexOf("/") + 1);
					depth2++;
				}
				return Integer.compare(depth2, depth1);
			});
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
