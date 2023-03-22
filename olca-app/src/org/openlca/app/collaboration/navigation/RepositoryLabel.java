package org.openlca.app.collaboration.navigation;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
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
				|| e.getLibrary().isPresent()
				|| e.isFromLibrary()
				|| !isNew(e))
			return null;
		return Images.get(e.getContent(), Overlay.ADDED);
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
		if (elem instanceof ModelElement e)
			return hasChanged(e);
		for (var child : elem.getChildren())
			if (hasChanged(child) || ((child instanceof ModelElement e) && isNew(e)))
				return true;
		return false;
	}

	private static boolean isNew(ModelElement e) {
		return !Repository.get().gitIndex.has(Cache.getPathCache(), e.getContent());
	}

	private static boolean hasChanged(ModelElement e) {
		if (isNew(e))
			return false;
		var entry = Repository.get().gitIndex.get(Cache.getPathCache(), e.getContent());
		return e.getContent().lastChange != entry.lastChange()
				|| e.getContent().version != entry.version();
	}

}
