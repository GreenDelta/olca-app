package org.openlca.app.cloud.navigation.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import com.greendelta.cloud.api.RepositoryConfig;
import com.greendelta.cloud.model.data.DatasetIdentifier;
import com.greendelta.cloud.util.WebRequests.WebRequestException;

public class CommitAction extends Action implements INavigationAction {

	private RepositoryConfig config;

	public CommitAction() {
		setText("#Commit...");
	}

	@Override
	public void run() {
		// TODO add monitor
		try {
			RepositoryClient client = new RepositoryClient(config);
			if (!upToDate(client))
				return;
			DiffIndex index = RepositoryNavigator.getDiffIndex();
			List<DiffResult> changes = createDifferences(index,
					index.getChanged());
			CommitDialog dialog = new CommitDialog(changes);
			dialog.setBlockOnOpen(true);
			if (dialog.open() != IDialogConstants.OK_ID)
				return;
			if (!upToDate(client))
				return;
			List<DiffResult> selection = dialog.getSelection();
			client.commit(dialog.getMessage(), loadEntities(selection),
					filterDeleted(selection));
			DiffIndexer indexer = new DiffIndexer(
					RepositoryNavigator.getDiffIndex());
			for (DiffResult result : selection)
				indexer.indexCommit(result.getIdentifier());
			RepositoryNavigator.refresh();
		} catch (WebRequestException e) {
			e.printStackTrace();
		}
	}

	private List<DiffResult> createDifferences(DiffIndex index,
			List<Diff> changes) {
		List<DiffResult> differences = new ArrayList<>();
		for (Diff diff : changes)
			differences.add(new DiffResult(diff));
		return differences;
	}

	private boolean upToDate(RepositoryClient client)
			throws WebRequestException {
		if (client.requestCommit())
			return true;
		Error.showBox(
				"#Rejected",
				"#Rejected - not up to date. Please fetch the latest changes from the repository first");
		return false;
	}

	private List<CategorizedEntity> loadEntities(List<DiffResult> changes) {
		Map<ModelType, Set<String>> map = new HashMap<>();
		for (DiffResult change : changes) {
			if (change.getType() == DiffResponse.DELETE_FROM_REMOTE)
				continue;
			Set<String> sublist = map.get(change.getIdentifier().getType());
			if (sublist == null)
				map.put(change.getIdentifier().getType(),
						sublist = new HashSet<>());
			sublist.add(change.getIdentifier().getRefId());
		}
		List<CategorizedEntity> entities = new ArrayList<>();
		for (ModelType type : map.keySet())
			entities.addAll(Database.createRootDao(type).getForRefIds(
					map.get(type)));
		return entities;
	}

	private List<DatasetIdentifier> filterDeleted(List<DiffResult> changes) {
		List<DatasetIdentifier> deleted = new ArrayList<>();
		for (DiffResult change : changes)
			if (change.getType() == DiffResponse.DELETE_FROM_REMOTE)
				deleted.add(change.getIdentifier());
		return deleted;
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		while (!(element instanceof RepositoryElement))
			element = element.getParent();
		config = (RepositoryConfig) element.getContent();
		if (config == null)
			return false;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return accept(elements.get(0));
	}

}
