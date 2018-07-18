package org.openlca.app.navigation.actions.cloud;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.openlca.app.App;
import org.openlca.app.M;
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
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;
import org.openlca.app.util.TimeEstimatingMonitor;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.database.IDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommitAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private IDatabase database;
	private DiffIndex index;
	private RepositoryClient client;
	private List<INavigationElement<?>> selection;

	public CommitAction() {
		setText(M.Commit);
	}

	@Override
	public void run() {
		Runner runner = new Runner();
		runner.run();
		if (!runner.upToDate && runner.error == null)
			Error.showBox(M.RejectMessage);
		else if (runner.error != null) {
			log.error("Error during commit action", runner.error);
			Error.showBox(runner.error.getMessage());
		} else if (runner.noChanges)
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
			App.runWithProgress(M.ComparingWithRepository, this::getDifferences);
			if (!upToDate)
				return;
			boolean doContinue = openCommitDialog();
			if (!doContinue)
				return;
			if (CloudPreference.doCheckReferences()) {
				App.runWithProgress(M.SearchingForReferencedChanges, this::searchForReferences);
				doContinue = showReferences();
				if (!doContinue)
					return;
			}
			if (CloudPreference.doCheckAgainstLibraries()) {
				App.runWithProgress(M.CheckingAgainstLibraries, this::checkAgainstLibraries);
				doContinue = openLibraryResultDialog();
			}
			if (!doContinue)
				return;
			checkUpToDate(client);
			if (!upToDate || error != null)
				return;
			Set<Dataset> datasets = new HashSet<>();
			for (DiffResult change : selected) {
				Dataset dataset = change.getDataset();
				if (change.getType() == DiffResponse.DELETE_FROM_REMOTE) {
					dataset.categories = new ArrayList<>(change.local.dataset.categories);
				}
				datasets.add(dataset);
			}
			commit(datasets);
			if (!upToDate)
				return;
			Navigator.refresh(Navigator.getNavigationRoot());
		}

		private void commit(Set<Dataset> datasets) {
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(UI.shell());
			try {
				dialog.run(true, false, (m) -> {
					TimeEstimatingMonitor monitor = new TimeEstimatingMonitor(m);
					monitor.beginTask(M.CommitingChanges, datasets.size());
					MutableInteger counter = new MutableInteger();
					try {
						client.commit(message, datasets, (dataset) -> {
							monitor.worked();
							counter.value++;
							if (counter.value == datasets.size()) {
								monitor.beginTask(M.WaitingForServerToIndexDatasets, IProgressMonitor.UNKNOWN);
							}
						});
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
					monitor.done();
				});
				dialog.run(true, false, (m) -> {
					TimeEstimatingMonitor monitor = new TimeEstimatingMonitor(m);
					monitor.beginTask(M.IndexingDatasets, datasets.size());
					orderResults();
					for (DiffResult change : selected) {
						Dataset dataset = change.getDataset();
						DiffType before = index.get(dataset.refId).type;
						if (before == DiffType.DELETED)
							index.remove(dataset.refId);
						else
							index.update(dataset, DiffType.NO_DIFF);
						monitor.worked();
					}
					index.commit();
					monitor.done();
				});
			} catch (Exception e) {
				error = e;
			}
		}

		// must order diffs by length of path, so when removing from index,
		// parent updating always works
		private void orderResults() {
			Collections.sort(selected, (d1, d2) -> {
				int depth1 = d1.getDataset().categories != null ? d1.getDataset().categories.size() : 0;
				int depth2 = d2.getDataset().categories != null ? d2.getDataset().categories.size() : 0;
				return Integer.compare(depth2, depth1);
			});
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

		private List<DiffResult> createDifferences(DiffIndex index, List<Diff> changes) {
			List<DiffResult> differences = new ArrayList<>();
			for (Diff diff : changes) {
				DiffResult diffResult = new DiffResult(diff);
				diffResult.ignoreRemote = true;
				differences.add(diffResult);
			}
			return differences;
		}

	}

	private class MutableInteger {

		public int value = 0;

	}

}
