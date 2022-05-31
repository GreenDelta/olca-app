package org.openlca.app.collaboration.navigation;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.collaboration.util.ObjectIds;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.GroupElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.util.Strings;

public class RepositoryLabel {

	public static final String CHANGED_STATE = "> ";

	public static Image getWithOverlay(INavigationElement<?> elem) {
		if (elem.getLibrary().isPresent())
			return null;
		if (!Repository.isConnected())
			return null;
		if (elem instanceof CategoryElement e && isZero(getRepositoryId(e)))
			return Images.getForCategory(e.getContent().modelType, Overlay.ADDED);
		if (elem instanceof ModelElement e && isZero(getRepositoryId(e)))
			return Images.get(e.getContent().type, Overlay.ADDED);
		return null;
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
		return null;
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
		return null;
	}

	private static boolean isZero(ObjectId id) {
		return ObjectIds.nullOrZero(id);
	}

	public static String getRepositoryText(DatabaseConfig dbConfig) {
		if (!Database.isActive(dbConfig))
			return null;
		if (!Repository.isConnected())
			return null;
		var repo = Repository.get();
		var ahead = repo.history.getAhead();
		var behind = repo.history.getBehind();
		var user = repo.user();
		var text = " [";
		if (!Strings.nullOrEmpty(user)) {
			text += user + "@";
		}
		text += repo.serverUrl + "/" + repo.repositoryId;
		if (!ahead.isEmpty()) {
			text += " ↑" + ahead.size();
		}
		if (!behind.isEmpty()) {
			text += " ↓" + behind.size();
		}
		return text + "]";
	}

	public static String getStateIndicator(INavigationElement<?> elem) {
		if (hasChanged(elem))
			return CHANGED_STATE;
		return null;
	}

	public static boolean hasChanged(INavigationElement<?> elem) {
		if (elem.getLibrary().isPresent())
			return false;
		if (!Repository.isConnected())
			return false;
		if (elem instanceof NavigationRoot)
			return false;
		if (elem instanceof DatabaseElement e) {
			if (!Database.isActive(e.getContent()))
				return false;
			for (var child : elem.getChildren())
				if (hasChanged(child))
					return true;
			return false;
		}
		if (elem instanceof GroupElement) {
			for (var child : elem.getChildren())
				if (hasChanged(child))
					return true;
			return false;
		}
		var repositoryId = getRepositoryId(elem);
		var workspaceId = getWorkspaceId(elem);
		var isNew = isZero(repositoryId);
		if (elem instanceof ModelTypeElement) {
			if (isNew != elem.getChildren().isEmpty())
				return true;
			for (var child : elem.getChildren())
				if (hasChanged(child))
					return true;
			var isChanged = !repositoryId.equals(workspaceId);
			return isChanged;
		}
		var hasChanged = !isNew && !repositoryId.equals(workspaceId);
		if (elem instanceof CategoryElement) {
			if (isNew != elem.getChildren().isEmpty())
				return true;
			for (var child : elem.getChildren())
				if (hasChanged(child))
					return true;
			return hasChanged;
		}
		return hasChanged;
	}

}
