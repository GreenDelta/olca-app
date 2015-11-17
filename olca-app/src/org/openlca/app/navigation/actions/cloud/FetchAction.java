package org.openlca.app.navigation.actions.cloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.openlca.app.App;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.CloudUtil.JsonLoader;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffIndexer;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.ui.CommitEntryDialog;
import org.openlca.app.cloud.ui.DiffDialog;
import org.openlca.app.cloud.ui.DiffNode;
import org.openlca.app.cloud.ui.DiffNodeBuilder;
import org.openlca.app.cloud.ui.DiffResult;
import org.openlca.app.cloud.ui.DiffResult.DiffResponse;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.CommitDescriptor;
import org.openlca.cloud.model.data.DatasetDescriptor;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.cloud.util.WebRequests.WebRequestException;

import com.google.gson.JsonObject;

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
		private DiffNode root;

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
			if (root != null) {
				JsonLoader loader = CloudUtil.getJsonLoader(client);
				DiffDialog dialog = new DiffDialog(root, loader::getLocalJson,
						loader::getRemoteJson);
				if (dialog.open() != IDialogConstants.OK_ID)
					return;
			}
			App.runWithProgress("#Fetching data", this::fetchData);
			afterFetchData();
		}

		private void fetchData() {
			try {
				List<DatasetDescriptor> toFetch = new ArrayList<>();
				Map<DatasetDescriptor, JsonObject> mergedData = new HashMap<>();
				for (DiffResult result : differences)
					if (result.getType() == DiffResponse.MODIFY_IN_LOCAL)
						toFetch.add(result.getDescriptor());
					else if (result.getType() == DiffResponse.ADD_TO_LOCAL)
						toFetch.add(result.getDescriptor());
					else if (result.getType() == DiffResponse.DELETE_FROM_LOCAL)
						toFetch.add(result.getDescriptor());
					else if (result.getType() == DiffResponse.CONFLICT)
						mergedData.put(result.getDescriptor(),
								result.getMergedData());
				client.fetch(toFetch, mergedData);
				updateIndex();
			} catch (WebRequestException e) {
				error = e;
			}
		}

		private void updateIndex() {
			List<DatasetDescriptor> addToIndex = new ArrayList<>();
			List<DatasetDescriptor> indexCreate = new ArrayList<>();
			List<DatasetDescriptor> indexModify= new ArrayList<>();
			List<DatasetDescriptor> indexDelete = new ArrayList<>();
			List<DatasetDescriptor> indexFetch = new ArrayList<>();
			List<DatasetDescriptor> removeFromIndex = new ArrayList<>();
			for (DiffResult result : differences)
				if (result.getType() == DiffResponse.NONE)
					if (result.remote != null && result.remote.isDeleted()
							&& result.local == null)
						removeFromIndex.add(result.getDescriptor());
					else
						indexFetch.add(result.getDescriptor());
				else if (result.getType() == DiffResponse.MODIFY_IN_LOCAL)
					indexFetch.add(result.getDescriptor());
				else if (result.getType() == DiffResponse.ADD_TO_LOCAL)
					addToIndex.add(result.getDescriptor());
				else if (result.getType() == DiffResponse.DELETE_FROM_LOCAL)
					removeFromIndex.add(result.getDescriptor());
				else if (result.getType() == DiffResponse.CONFLICT)
					if (result.local.type == DiffType.CHANGED) {
						if (result.remote.isDeleted()) {
							if (result.overwriteRemoteChanges())
								indexCreate.add(result.getDescriptor());
							else if (result.overwriteLocalChanges())
								removeFromIndex.add(result.getDescriptor());
						} else {
							if (result.overwriteRemoteChanges()) // merged
								indexModify.add(result.getDescriptor());
							else if (result.overwriteLocalChanges())
								indexFetch.add(result.getDescriptor());							
						}
					} else if (result.local.type == DiffType.DELETED)
						if (result.overwriteRemoteChanges())
							indexDelete.add(result.getDescriptor());
						else if (result.overwriteLocalChanges())
							indexFetch.add(result.getDescriptor());
			DiffIndexer indexer = new DiffIndexer(index);
			indexer.addToIndex(addToIndex);
			indexer.indexCreate(indexCreate);
			indexer.indexModify(indexModify);
			indexer.indexDelete(indexDelete);
			indexer.indexFetch(indexFetch);
			indexer.removeFromIndex(removeFromIndex);
		}

		private void afterFetchData() {
			if (error != null)
				return;
			Navigator.refresh(Navigator.getNavigationRoot());
		}

		private void showNoChangesBox() {
			Info.showBox("#Up to date - No changes fetched");
		}

		private List<DiffResult> createDifferences(
				List<FetchRequestData> remotes) {
			List<DiffResult> differences = new ArrayList<>();
			for (FetchRequestData identifier : remotes) {
				Diff local = index.get(identifier.getRefId());
				differences.add(new DiffResult(identifier, local));
			}
			return differences;
		}

	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		client = Database.getRepositoryClient();
		if (client == null)
			return false;
		index = Database.getDiffIndex();
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
