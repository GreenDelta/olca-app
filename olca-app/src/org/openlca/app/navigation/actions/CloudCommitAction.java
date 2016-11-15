package org.openlca.app.navigation.actions;

import org.openlca.app.M;

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
import org.openlca.app.cloud.ui.ReferencesResultDialog;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.cloud.ui.diff.DiffNode;
import org.openlca.app.cloud.ui.diff.DiffNodeBuilder;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
import org.openlca.app.cloud.ui.library.LibraryResultDialog;
import org.openlca.app.cloud.ui.preferences.CloudPreference;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;
import org.openlca.cloud.api.CommitInvocation;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;

class CloudCommitAction extends Action implements INavigationAction {

	private IDatabase database;
	private DiffIndex index;
	private RepositoryClient client;
	private List<INavigationElement<?>> selection;

	public CloudCommitAction() {
		setText(M.Commit);
	}

	@Override
	public void run() {
		Runner runner = new Runner();
		runner.run();
		if (runner.error != null)
			Error.showBox(runner.error.getMessage());
		else if (!runner.upToDate)
			Error.showBox(M.RejectMessage);
		else if (runner.noChanges)
			Info.showBox(M.NoChangesInLocalDb);
		HistoryView.refresh();
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!Database.isConnected())
			return false;
		index = Database.getDiffIndex();
		client = Database.getRepositoryClient();
		database = Database.get();
		selection = Collections.singletonList(element);
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		if (!Database.isConnected())
			return false;
		index = Database.getDiffIndex();
		client = Database.getRepositoryClient();
		database = Database.get();
		selection = elements;
		return true;
	}

	private class Runner {

		private boolean upToDate;
		private boolean noChanges;
		private String message;
		private List<DiffResult> changes;
		private List<DiffResult> selected;
		private List<DiffResult> references;
		private Map<Dataset, String> checkResult;
		private Exception error;

		public void run() {
			App.runWithProgress(M.ComparingWithRepository,
					this::getDifferences);
			if (!upToDate)
				return;
			boolean doContinue = openCommitDialog();
			if (!doContinue)
				return;
			App.runWithProgress(M.SearchingForReferencedChanges,
					this::searchForReferences);
			doContinue = showReferences();
			if (!doContinue)
				return;
			if (CloudPreference.doCheckAgainstLibraries()) {
				App.runWithProgress(M.CheckingAgainstLibraries, this::checkAgainstLibraries);
				doContinue = openLibraryResultDialog();
			}
			if (!doContinue)
				return;
			App.runWithProgress(M.CommitingChanges, this::commit);
			if (!upToDate)
				return;
			Navigator.refresh(Navigator.getNavigationRoot());
		}

		private void getDifferences() {
			checkUpToDate(client);
			if (!upToDate)
				return;
			changes = createDifferences(index, index.getChanged());
		}

		private void checkUpToDate(RepositoryClient client) {
			try {
				upToDate = client.requestCommit();
			} catch (Exception e) {
				error = e;
				upToDate = false;
			}
		}

		private boolean openCommitDialog() {
			DiffNode node = new DiffNodeBuilder(Database.get(), Database.getDiffIndex()).build(changes);
			if (node == null) {
				noChanges = true;
				return false;
			}
			CommitDialog dialog = new CommitDialog(node, client);
			dialog.setBlockOnOpen(true);
			Set<String> refIds = new RefIdListBuilder(selection, changes, index).build();
			dialog.setInitialSelection(refIds);
			if (dialog.open() != IDialogConstants.OK_ID)
				return false;
			message = dialog.getMessage();
			selected = dialog.getSelected();
			return true;
		}

		private void searchForReferences() {
			ReferenceSearcher searcher = new ReferenceSearcher(database, index);
			references = searcher.run(selected);
		}

		private boolean showReferences() {
			if (error != null)
				return false;
			if (references == null || references.isEmpty())
				return true;
			DiffNode node = new DiffNodeBuilder(Database.get(), Database.getDiffIndex()).build(references);
			ReferencesResultDialog dialog = new ReferencesResultDialog(node, client);
			if (dialog.open() != IDialogConstants.OK_ID)
				return false;
			selected.addAll(dialog.getSelected());
			return true;
		}

		private boolean openLibraryResultDialog() {
			if (error != null)
				return false;
			if (checkResult == null || checkResult.isEmpty())
				return true;
			LibraryResultDialog libraryDialog = new LibraryResultDialog(checkResult);
			if (libraryDialog.open() != IDialogConstants.OK_ID)
				return false;
			return true;
		}

		private void checkAgainstLibraries() {
			Set<Dataset> datasets = new HashSet<>();
			for (DiffResult result : selected)
				datasets.add(result.getDataset());
			try {
				checkResult = client.performLibraryCheck(datasets);
			} catch (Exception e) {
				error = e;
			}
		}

		private void commit() {
			try {
				checkUpToDate(client);
				if (!upToDate || error != null)
					return;
				CommitInvocation commit = client.createCommitInvocation();
				commit.setCommitMessage(message);
				putChanges(selected, commit);
				client.execute(commit);
				indexCommit();
			} catch (Exception e) {
				error = e;
			}
		}

		private void indexCommit() {
			orderResults();
			for (DiffResult change : selected) {
				Dataset dataset = change.getDataset();
				DiffType before = index.get(dataset.refId).type;
				if (before == DiffType.DELETED)
					index.remove(dataset.refId);
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
				String path = d1.getDataset().fullPath;
				while (path.contains("/")) {
					path = path.substring(path.indexOf("/") + 1);
					depth1++;
				}
				int depth2 = 0;
				path = d2.getDataset().fullPath;
				while (path.contains("/")) {
					path = path.substring(path.indexOf("/") + 1);
					depth2++;
				}
				return Integer.compare(depth2, depth1);
			});
		}

		private List<DiffResult> createDifferences(DiffIndex index,
				List<Diff> changes) {
			List<DiffResult> differences = new ArrayList<>();
			for (Diff diff : changes) {
				DiffResult diffResult =  new DiffResult(diff);
				diffResult.ignoreRemote = true;
				differences.add(diffResult);
			}
			return differences;
		}

		private void putChanges(List<DiffResult> changes,
				CommitInvocation commit) {
			for (DiffResult change : changes)
				if (change.getType() == DiffResponse.DELETE_FROM_REMOTE) {
					Dataset dataset = change.getDataset();
					dataset.fullPath = change.local.dataset.fullPath;
					commit.putForRemoval(dataset);
				} else {
					ModelType type = change.getDataset().type;
					String refId = change.getDataset().refId;
					commit.put(Database.createCategorizedDao(type).getForRefId(refId));
				}
		}

	}

}
