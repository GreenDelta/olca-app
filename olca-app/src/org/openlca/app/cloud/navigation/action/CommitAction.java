package org.openlca.app.cloud.navigation.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.openlca.app.App;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffIndexer;
import org.openlca.app.cloud.navigation.RepositoryElement;
import org.openlca.app.cloud.navigation.RepositoryNavigator;
import org.openlca.app.cloud.ui.CommitDialog;
import org.openlca.app.cloud.ui.DiffResult;
import org.openlca.app.cloud.ui.DiffResult.DiffResponse;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Error;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;

import com.greendelta.cloud.api.RepositoryClient;
import com.greendelta.cloud.model.data.DatasetDescriptor;
import com.greendelta.cloud.util.WebRequests.WebRequestException;

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
			Error.showBox(
					"#Rejected",
					"#Rejected - not up to date. Please fetch the latest changes from the repository first");
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
			CommitDialog dialog = new CommitDialog(changes);
			dialog.setBlockOnOpen(true);
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			App.runWithProgress("#Commiting changes", this::commit);
			afterCommit();
		}

		private void commit() {
			try {
				checkUpToDate(client);
				if (!canContinue())
					return;
				client.commit(message, loadEntities(changes),
						filterDeleted(changes));
				DiffIndexer indexer = new DiffIndexer(index);
				for (DiffResult result : changes)
					indexer.indexCommit(result.getDescriptor());
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

		private List<CategorizedEntity> loadEntities(List<DiffResult> changes) {
			Map<ModelType, Set<String>> map = new HashMap<>();
			for (DiffResult change : changes) {
				if (change.getType() == DiffResponse.DELETE_FROM_REMOTE)
					continue;
				Set<String> sublist = map.get(change.getDescriptor().getType());
				if (sublist == null)
					map.put(change.getDescriptor().getType(),
							sublist = new HashSet<>());
				sublist.add(change.getDescriptor().getRefId());
			}
			List<CategorizedEntity> entities = new ArrayList<>();
			for (ModelType type : map.keySet())
				entities.addAll(Database.createRootDao(type).getForRefIds(
						map.get(type)));
			return entities;
		}

		private List<DatasetDescriptor> filterDeleted(List<DiffResult> changes) {
			List<DatasetDescriptor> deleted = new ArrayList<>();
			for (DiffResult change : changes)
				if (change.getType() == DiffResponse.DELETE_FROM_REMOTE)
					deleted.add(change.getDescriptor());
			return deleted;
		}
	}

}
