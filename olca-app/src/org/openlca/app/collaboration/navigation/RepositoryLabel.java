package org.openlca.app.collaboration.navigation;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.git.util.Constants;
import org.openlca.util.Strings;

public class RepositoryLabel {

	public static final String CHANGED_STATE = "> ";

	public static Image getWithOverlay(INavigationElement<?> elem) {
		if (!(elem instanceof ModelElement e)
				|| !Repository.isConnected()
				|| elem.getLibrary().isPresent()
				|| e.isFromLibrary()
				|| !isZero(getRepositoryId(elem)))
			return null;
		return Images.get(e.getContent().type, Overlay.ADDED);
	}

	private static ObjectId getRepositoryId(INavigationElement<?> elem) {
		if (elem instanceof DatabaseElement)
			return Repository.get().workspaceIds.getHead("");
		if (elem instanceof ModelTypeElement e)
			return Repository.get().workspaceIds.getHead(e.getContent().name());
		if (elem instanceof CategoryElement e) {
			var path = Repository.get().workspaceIds.getPath(e.getContent());
			return Repository.get().workspaceIds.getHead(path);
		}
		if (elem instanceof ModelElement e) {
			var path = Repository.get().workspaceIds.getPath(Cache.getPathCache(), e.getContent());
			return Repository.get().workspaceIds.getHead(path);
		}
		return ObjectId.zeroId();
	}

	private static ObjectId getWorkspaceId(INavigationElement<?> elem) {
		if (elem instanceof DatabaseElement)
			return Repository.get().workspaceIds.get("");
		if (elem instanceof ModelTypeElement e)
			return Repository.get().workspaceIds.get(e.getContent());
		if (elem instanceof CategoryElement e)
			return Repository.get().workspaceIds.get(e.getContent());
		if (elem instanceof ModelElement e)
			return Repository.get().workspaceIds.get(Cache.getPathCache(), e.getContent());
		return ObjectId.zeroId();
	}

	private static boolean isZero(ObjectId id) {
		return ObjectId.zeroId().equals(id);
	}

	public static String getRepositoryText(DatabaseConfig dbConfig) {
		if (!Database.isActive(dbConfig))
			return null;
		if (!Repository.isConnected())
			return null;
		var repo = Repository.get();
		var ahead = repo.localHistory.getAheadOf(Constants.REMOTE_REF);
		var behind = repo.localHistory.getBehindOf(Constants.REMOTE_REF);
		var user = repo.user();
		var text = " [";
		if (!Strings.nullOrEmpty(user)) {
			text += user + "@";
		}
		text += repo.client.serverUrl + "/" + repo.client.repositoryId;
		if (!ahead.isEmpty()) {
			text += " ↑" + ahead.size();
		}
		if (!behind.isEmpty()) {
			text += " ↓" + behind.size();
		}
		return text + "]";
	}

	public static String getStateIndicator(INavigationElement<?> elem) {
		if (!hasChanged(elem))
			return null;
		// if (!hasNonLibraryContent(elem))
		// return null;
		if (elem instanceof ModelElement && isZero(getRepositoryId(elem)))
			return null;
		return CHANGED_STATE;
	}

	public static boolean hasChanged(INavigationElement<?> elem) {
		if (elem.getLibrary().isPresent())
			return false;
		if (!Repository.isConnected())
			return false;
		if (elem instanceof NavigationRoot)
			return false;
		if (elem instanceof DatabaseElement e && !Database.isActive(e.getContent()))
			return false;
		var repositoryId = getRepositoryId(elem);
		var workspaceId = getWorkspaceId(elem);
		var isNew = isZero(repositoryId);
		if (isNew) {
			if (elem instanceof ModelElement m && !m.isFromLibrary())
				return true;
			if (elem instanceof CategoryElement) {
				for (var child : elem.getChildren())
					if (hasChanged(child))
						return true;
				return false;
			}
		}
		var hasChanged = !repositoryId.equals(workspaceId);
		if (!isNew && hasChanged)
			return true;
		for (var child : elem.getChildren())
			if (hasChanged(child))
				return true;
		return false;
	}

}
