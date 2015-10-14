package org.openlca.app.cloud.navigation.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.openlca.app.App;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffIndexer;
import org.openlca.app.cloud.navigation.RepositoryElement;
import org.openlca.app.cloud.navigation.RepositoryNavigator;
import org.openlca.app.cloud.ui.CommitEntryDialog;
import org.openlca.app.cloud.ui.DiffDialog;
import org.openlca.app.cloud.ui.DiffNodeBuilder;
import org.openlca.app.cloud.ui.DiffNodeBuilder.Node;
import org.openlca.app.cloud.ui.DiffResult;
import org.openlca.app.cloud.ui.DiffResult.DiffResponse;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;

import com.greendelta.cloud.api.RepositoryClient;
import com.greendelta.cloud.model.data.CommitDescriptor;
import com.greendelta.cloud.model.data.DatasetDescriptor;
import com.greendelta.cloud.model.data.FetchRequestData;
import com.greendelta.cloud.util.WebRequests.WebRequestException;

public class FetchAction extends Action implements INavigationAction {

	private RepositoryClient client;
	private DiffIndex index;

	public FetchAction() {
		setText("#Fetch...");
	}

	@Override
	public void run() {
		Runner runner = new Runner();
		runner.run();
		if (runner.error != null)
			Error.showBox(runner.error.getMessage());
	}

	private class Runner {

		private List<CommitDescriptor> commits;
		private List<DiffResult> differences;
		private WebRequestException error;
		private Node root;

		public void run() {
			App.runWithProgress("#Fetching commits", this::fetchCommits);
			showCommitEntries();
		}

		private void fetchCommits() {
			try {
				commits = client.fetchNewCommitHistory();
			} catch (WebRequestException e) {
				error = e;
			}
		}

		private void showCommitEntries() {
			if (error != null)
				return;
			if (commits.isEmpty()) {
				showNoChangesBox();
				return;
			}
			CommitEntryDialog dialog = new CommitEntryDialog(commits, client);
			dialog.open();
			App.runWithProgress("#Fetching changes", this::requestFetch);
			showDifferences();
		}

		private void requestFetch() {
			try {
				List<FetchRequestData> descriptors = client.requestFetch();
				differences = createDifferences(descriptors);
				root = new DiffNodeBuilder(client.getConfig().getDatabase(),
						index).build(differences);
			} catch (WebRequestException e) {
				error = e;
			}
		}

		private void showDifferences() {
			if (error != null)
				return;
			DiffDialog dialog = new DiffDialog(root);
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			App.runWithProgress("#Fetching data", this::fetchData);
			afterFetchData();
		}

		private void fetchData() {
			try {
				// TODO apply merge results for conflicts
				client.fetch();
				DiffIndexer indexer = new DiffIndexer(index);
				indexer.addToIndex(extractNew());
				// TODO apply merged data to index
			} catch (WebRequestException e) {
				error = e;
			}
		}

		private void afterFetchData() {
			if (error != null)
				return;
			RepositoryNavigator.refresh();
			Navigator.refresh();
		}

		private List<DatasetDescriptor> extractNew() {
			List<DatasetDescriptor> identifiers = new ArrayList<>();
			for (DiffResult result : differences)
				if (result.getType() == DiffResponse.ADD_TO_LOCAL)
					identifiers.add(result.getDescriptor());
			return identifiers;
		}

		private void showNoChangesBox() {
			Info.showBox("#Up to date - No changes fetched");
		}

		private List<DiffResult> createDifferences(
				List<FetchRequestData> remotes) {
			List<DiffResult> differences = new ArrayList<>();
			for (FetchRequestData identifier : remotes) {
				Diff local = index.get(identifier.getRefId());
				if (identifier.isDeleted() && local == null)
					continue;
				if (local == null) {
					differences.add(new DiffResult(identifier));
					continue;
				}
				differences.add(new DiffResult(identifier, local));
			}
			return differences;
		}

	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof RepositoryElement))
			return false;
		client = RepositoryNavigator.getClient();
		if (client == null)
			return false;
		index = RepositoryNavigator.getDiffIndex();
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
