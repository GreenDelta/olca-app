package org.openlca.app.navigation.actions.cloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.ui.DiffDialog;
import org.openlca.app.cloud.ui.commits.CommitEntryDialog;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.cloud.ui.compare.json.JsonUtil;
import org.openlca.app.cloud.ui.diff.DiffNode;
import org.openlca.app.cloud.ui.diff.DiffNodeBuilder;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Commit;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.core.model.Process;
import org.openlca.util.KeyGen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class FetchAction extends Action implements INavigationAction {

	private RepositoryClient client;
	private DiffIndex index;

	public FetchAction() {
		setText(M.Fetch);
	}

	public static void main(String[] args) {
		KeyGen.get("");
	}

	@Override
	public void run() {
		Runner runner = new Runner();
		runner.run();
		if (runner.error != null)
			Error.showBox(runner.error.getMessage());
		HistoryView.refresh();
	}

	private class Runner {

		private List<Commit> commits;
		private List<DiffResult> differences;
		private Exception error;
		private DiffNode root;

		public void run() {
			App.runWithProgress(M.FetchingCommits, this::fetchCommits);
			boolean doContinue = showCommitEntries();
			if (!doContinue)
				return;
			App.runWithProgress(M.FetchingChanges, this::requestFetch);
			doContinue = showDifferences();
			if (!doContinue)
				return;
			App.runWithProgress(M.FetchingData, this::fetchData);
			if (error != null)
				return;
			Navigator.refresh(Navigator.getNavigationRoot());
		}

		private void fetchCommits() {
			try {
				commits = client.fetchNewCommitHistory();
			} catch (Exception e) {
				error = e;
			}
		}

		private boolean showCommitEntries() {
			if (error != null)
				return false;
			if (commits.isEmpty()) {
				showNoChangesBox();
				return false;
			}
			CommitEntryDialog dialog = new CommitEntryDialog(commits, client);
			if (dialog.open() != IDialogConstants.OK_ID)
				return false;
			return true;
		}

		private void requestFetch() {
			try {
				List<FetchRequestData> descriptors = client.requestFetch();
				differences = createDifferences(descriptors);
				root = new DiffNodeBuilder(client.getConfig().getDatabase(), index).build(differences);
			} catch (Exception e) {
				error = e;
			}
		}

		private boolean showDifferences() {
			if (error != null)
				return false;
			if (root != null) {
				JsonLoader loader = CloudUtil.getJsonLoader(client);
				DiffDialog dialog = new DiffDialog(root, loader);
				if (dialog.open() != IDialogConstants.OK_ID)
					return false;
			}
			return true;
		}

		private void fetchData() {
			List<Dataset> toFetch = new ArrayList<>();
			Map<Dataset, JsonObject> mergedData = new HashMap<>();
			for (DiffResult result : differences) {
				Dataset dataset = result.getDataset();
				JsonObject data = result.getMergedData();
				String type = JsonUtil.getString(data, "@type");
				if (Process.class.getSimpleName().equals(type)) {
					joinExchanges(data);
				}
				if (data != null) {
					mergedData.put(dataset, data);
				} else {
					toFetch.add(dataset);
				}
			}
			try {
				Database.getIndexUpdater().disable();
				client.fetch(toFetch, mergedData);
				Database.getIndexUpdater().enable();
				FetchIndexHelper.index(differences, index);
			} catch (Exception e) {
				error = e;
			}
		}

		// for ease of display, the exchanges list was split into inputs and
		// outputs, these two lists need to be merged
		private void joinExchanges(JsonObject data) {
			JsonArray exchanges = new JsonArray();
			JsonArray inputs = data.getAsJsonArray("inputs");
			JsonArray outputs = data.getAsJsonArray("outputs");
			if (inputs != null) {
				for (JsonElement elem : inputs) {
					JsonObject e = elem.getAsJsonObject();
					exchanges.add(e);
				}
			}
			if (outputs != null) {
				for (JsonElement elem : outputs) {
					JsonObject e = elem.getAsJsonObject();
					exchanges.add(e);
				}
			}
			data.remove("inputs");
			data.remove("outputs");
			data.add("exchanges", exchanges);
		}

		private void showNoChangesBox() {
			Info.showBox(M.UpToDate);
		}

		private List<DiffResult> createDifferences(
				List<FetchRequestData> remotes) {
			List<DiffResult> differences = new ArrayList<>();
			for (FetchRequestData identifier : remotes) {
				Diff local = index.get(identifier.refId);
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
