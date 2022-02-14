package org.openlca.app.collaboration.ui.navigation.actions;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.lib.ObjectId;
import org.openlca.app.collaboration.ui.io.GitStore;
import org.openlca.app.collaboration.util.Constants;
import org.openlca.app.collaboration.util.WorkspaceDiffs;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.core.model.ModelType;
import org.openlca.git.model.Entry.EntryType;
import org.openlca.jsonld.input.JsonImport;
import org.openlca.jsonld.input.UpdateMode;

public class MergeAction extends Action implements INavigationAction {

	private static final ModelType[] typeOrder = new ModelType[] {
			ModelType.DQ_SYSTEM,
			ModelType.LOCATION,
			ModelType.ACTOR,
			ModelType.SOURCE,
			ModelType.PARAMETER,
			ModelType.UNIT_GROUP,
			ModelType.FLOW_PROPERTY,
			ModelType.CURRENCY,
			ModelType.FLOW,
			ModelType.IMPACT_CATEGORY,
			ModelType.IMPACT_METHOD,
			ModelType.SOCIAL_INDICATOR,
			ModelType.PROCESS,
			ModelType.PRODUCT_SYSTEM,
			ModelType.PROJECT,
			ModelType.RESULT
		};
	
	@Override
	public String getText() {
		return "Merge...";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.MERGE.descriptor();
	}

	@Override
	public void run() {
		var commits = Repository.get().commits;
		var localCommitId = commits.resolve(Constants.LOCAL_BRANCH);
		var remoteCommitId = commits.resolve(Constants.REMOTE_BRANCH);
		if (!hasChanges(localCommitId, remoteCommitId)) {
			MsgBox.info("No changes to merge");
			return;
		}
		// TODO handle conflicts (C)
		runImport(localCommitId, remoteCommitId);
		updateHeadRef(remoteCommitId);
		Cache.evictAll();
		Actions.refresh();
	}

	boolean hasChanges(String localCommitId, String remoteCommitId) {
		var behind = Repository.get().commits.find()
				.after(localCommitId)
				.until(remoteCommitId)
				.all();
		return !behind.isEmpty();
	}

	private void runImport(String localCommitId, String remoteCommitId) {
		var commit = localCommitId != null
				? Repository.get().commits.get(localCommitId)
				: null;
		var changed = WorkspaceDiffs.get(commit).stream()
				.map(d -> d.path())
				.collect(Collectors.toSet());
		var workspaceIds = Repository.get().workspaceIds;
		var jsonStore = new GitStore(Repository.get().git, localCommitId, remoteCommitId);
		var jsonImport = new JsonImport(jsonStore, Database.get());
		jsonImport.setUpdateMode(UpdateMode.ALWAYS);
		Database.getWorkspaceIdUpdater().disable();
		for (var type : typeOrder) {
			var changes = jsonStore.getChanges(type);
			for (var change : changes) {
				jsonImport.run(type, change.refId);
				workspaceIds.put(change.fullPath, change.objectId);
				changed.remove(change.fullPath); // TODO (C)
			}
		}
		Database.getWorkspaceIdUpdater().enable();
		updateCategoryIds(remoteCommitId, "");
		workspaceIds.putRoot(ObjectId.fromString(remoteCommitId));
		changed.forEach(path -> workspaceIds.invalidate(path));
		try {
			workspaceIds.save();
		} catch (IOException e) {
			Actions.handleException("Error saving workspace ids", e);
		}
	}

	private void updateCategoryIds(String remoteCommitId, String path) {
		Repository.get().entries.find().commit(remoteCommitId).path(path).all().forEach(entry -> {
			if (entry.typeOfEntry == EntryType.DATASET)
				return;
			Repository.get().workspaceIds.put(entry.fullPath, entry.objectId);
			updateCategoryIds(remoteCommitId, entry.fullPath);
		});
	}

	private void updateHeadRef(String remoteCommitId) {
		try {
			var update = Repository.get().git.updateRef(Constants.LOCAL_BRANCH);
			update.setNewObjectId(ObjectId.fromString(remoteCommitId));
			update.update();
		} catch (IOException e) {
			Actions.handleException("Error merging commits", e);
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return Repository.isConnected();
	}

}
