package org.openlca.app.navigation.actions.cloud;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.ui.DiffDialog;
import org.openlca.app.cloud.ui.FetchNotifierMonitor;
import org.openlca.app.cloud.ui.commits.CommitEntryDialog;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.cloud.ui.compare.json.JsonUtil;
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
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Commit;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.cloud.model.data.FileReference;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class FetchAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(FetchAction.class);
	private RepositoryClient client;
	private DiffIndex index;

	public FetchAction() {
		setText(M.Fetch);
	}

	@Override
	public void run() {
		Runner runner = new Runner();
		runner.run();
		if (runner.error != null) {
			log.error("Error during fetch action", runner.error);
			Error.showBox(runner.error.getMessage());
		}
		Navigator.refresh();
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
			fetchData();
			if (error != null)
				return;
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
				Set<FetchRequestData> descriptors = client.requestFetch();
				differences = createDifferences(descriptors);
				root = new DiffNodeBuilder(client.getConfig().database, index).build(differences);
			} catch (Exception e) {
				error = e;
			}
		}

		private boolean showDifferences() {
			if (error != null)
				return false;
			if (root == null)
				return false;
			if (root.children.isEmpty())
				return true;
			JsonLoader loader = CloudUtil.getJsonLoader(client);
			DiffDialog dialog = new DiffDialog(root, loader);
			if (dialog.open() != IDialogConstants.OK_ID)
				return false;
			return true;
		}

		private void fetchData() {
			Set<FileReference> toFetch = new HashSet<>();
			Map<Dataset, JsonObject> mergedData = new HashMap<>();
			for (DiffResult result : differences) {
				if (result.getType() == DiffResponse.NONE)
					continue;
				Dataset dataset = result.getDataset();
				JsonObject data = result.getMergedData();
				String type = JsonUtil.getString(data, "@type");
				if (Process.class.getSimpleName().equals(type)) {
					joinExchanges(data);
				}
				if (data != null) {
					mergedData.put(dataset, data);
				} else if (!result.overwriteRemoteChanges()) {
					toFetch.add(dataset.asFileReference());
				}
			}
			try {
				Database.getIndexUpdater().disable();
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(UI.shell());
				dialog.run(true, false, new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor m) throws InvocationTargetException, InterruptedException {
						try {
							m.beginTask(M.Preparing, IProgressMonitor.UNKNOWN);
							FetchNotifierMonitor monitor = new FetchNotifierMonitor(m, M.FetchingData);
							client.fetch(toFetch, mergedData, monitor);
							monitor.beginTask("#Indexing datasets", differences.size());
							FetchIndexHelper.index(differences, index, (e) -> monitor.worked());
							monitor.done();
						} catch (WebRequestException e) {
							throw new InvocationTargetException(e, e.getMessage());
						}
					}
				});
			} catch (Exception e) {
				error = e;
			} finally {
				Database.getIndexUpdater().enable();
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

		private List<DiffResult> createDifferences(Set<FetchRequestData> remotes) {
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
