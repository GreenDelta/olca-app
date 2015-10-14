package org.openlca.app.cloud.navigation.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.openlca.app.util.Info;

import com.greendelta.cloud.api.RepositoryClient;
import com.greendelta.cloud.model.data.CommitDescriptor;
import com.greendelta.cloud.model.data.DatasetDescriptor;
import com.greendelta.cloud.model.data.FetchRequestData;
import com.greendelta.cloud.util.WebRequests.WebRequestException;

public class FetchAction extends Action implements INavigationAction {

	private RepositoryClient client;

	public FetchAction() {
		setText("#Fetch...");
	}

	@Override
	public void run() {
		try {
			// TODO add monitor
			List<CommitDescriptor> commits = client.fetchNewCommitHistory();
			if (commits.isEmpty()) {
				showNoChangesBox();
				return;
			}
			new CommitEntryDialog(commits).open();
			List<FetchRequestData> descriptors = client.requestFetch();
			DiffIndex index = RepositoryNavigator.getDiffIndex();
			List<DiffResult> differences = createDifferences(index, descriptors);
			Node root = new DiffNodeBuilder(client.getConfig().getDatabase(),
					RepositoryNavigator.getDiffIndex()).build(differences);
			// TODO
			DiffDialog dialog = new DiffDialog(root);
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			// TODO apply merge results for conflicts
			client.fetch();
			DiffIndexer indexer = new DiffIndexer(index);
			indexer.addToIndex(extractNew(differences));
			// TODO apply merged data to index
			RepositoryNavigator.refresh();
			Navigator.refresh();
		} catch (WebRequestException e) {
			// TODO handle errors
			Info.showBox(e.getMessage());
			client.getConfig().disconnect();
		}
	}

	private List<DatasetDescriptor> extractNew(
			List<DiffResult> results) {
		List<DatasetDescriptor> descriptors = new ArrayList<>();
		for (DiffResult result : results)
			if (result.getType() == DiffResponse.ADD_TO_LOCAL)
				descriptors.add(result.getDescriptor());
		return descriptors;
	}

	private void showNoChangesBox() {
		Info.showBox("#Up to date - No changes fetched");
	}

	private List<DiffResult> createDifferences(DiffIndex index,
			List<FetchRequestData> remotes) {
		List<DiffResult> differences = new ArrayList<>();
		for (FetchRequestData descriptor : remotes) {
			Diff local = index.get(descriptor.getRefId());
			if (descriptor.isDeleted() && local == null)
				continue;
			if (local == null) {
				differences.add(new DiffResult(descriptor));
				continue;
			}
			differences.add(new DiffResult(descriptor, local));
		}
		return differences;
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof RepositoryElement))
			return false;
		client = ((RepositoryElement) element).getContent();
		if (client == null)
			return false;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
