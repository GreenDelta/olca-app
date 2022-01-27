package org.openlca.app.collaboration.ui.navigation;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.collaboration.util.Constants;
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

/**
 * known limitations: if a new model is created, the parent categories will be
 * marked as changed (correctly), if the new model is deleted the parent
 * categories will still be marked as changed (incorrectly), because we
 * invalidated the categories and dont calculate new tree ids
 */
public class RepositoryLabel {

	public static final String CHANGED_STATE = "> ";

	public static Image getWithOverlay(INavigationElement<?> elem) {
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
			return Repository.get().ids.get("");
		if (elem instanceof ModelTypeElement e)
			return Repository.get().ids.get(e.getContent().name());
		if (elem instanceof CategoryElement e) {
			var path = Repository.get().workspaceIds.getPath(e.getContent());
			return Repository.get().ids.get(path);
		}
		if (elem instanceof ModelElement e) {
			var path = Repository.get().workspaceIds.getPath(Cache.getPathCache(), e.getContent());
			return Repository.get().ids.get(path);
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
		var config = Repository.get().config;
		var commits = Repository.get().commits;
		var localCommitId = commits.resolve(Constants.LOCAL_BRANCH);
		var remoteCommitId = commits.resolve(Constants.REMOTE_BRANCH);
		var ahead = commits.find()
				.after(remoteCommitId)
				.until(localCommitId)
				.all().size();
		var behind = commits.find()
				.after(localCommitId)
				.until(remoteCommitId)
				.all().size();
		var text = " [" + config.serverUrl + " " + config.repositoryId;
		if (ahead > 0) {
			text += " ↑" + ahead;
		}
		if (behind > 0) {
			text += " ↓" + behind;
		}
		return text + "]";
	}

	public static String getStateIndicator(INavigationElement<?> elem) {
		if (indicateChangedState(elem))
			return CHANGED_STATE;
		return null;
	}

	private static boolean indicateChangedState(INavigationElement<?> elem) {
		if (!Repository.isConnected())
			return false;
		if (elem instanceof NavigationRoot)
			return false;
		if (elem instanceof DatabaseElement e) {
			if (!Database.isActive(e.getContent()))
				return false;
			for (var child : elem.getChildren())
				if (indicateChangedState(child))
					return true;
			return false;
		}
		if (elem instanceof GroupElement) {
			for (var child : elem.getChildren())
				if (indicateChangedState(child))
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
				if (indicateChangedState(child))
					return true;
			var isChanged = !repositoryId.equals(workspaceId);
			return isChanged;
		}
		var isChanged = !isNew && !repositoryId.equals(workspaceId);
		if (elem instanceof CategoryElement) {
			if (isNew != elem.getChildren().isEmpty())
				return true;
			for (var child : elem.getChildren())
				if (indicateChangedState(child))
					return true;
			return isChanged;
		}
		return isChanged;
	}

	public static Font getFont(INavigationElement<?> elem) {
		// if (!isTracked(elem))
		// return UI.italicFont();
		return null;
	}

	public static Color getForeground(INavigationElement<?> elem) {
		// if (!isTracked(elem))
		// return Colors.get(85, 85, 85);
		return null;
	}

}
