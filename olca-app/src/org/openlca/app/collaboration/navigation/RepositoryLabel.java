package org.openlca.app.collaboration.navigation;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.collaboration.navigation.NavElement.ElementType;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryDirElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Path;
import org.openlca.jsonld.LibraryLink;
import org.openlca.util.Strings;

public class RepositoryLabel {

	public static final String CHANGED_STATE = "> ";

	public static Image getWithOverlay(INavigationElement<?> elem) {
		if (Database.get() == null || !Repository.isConnected())
			return null;
		if (elem instanceof ModelElement e
				&& !e.getLibrary().isPresent()
				&& !e.isFromLibrary()
				&& isNew(NavCache.get(e)))
			return Images.get(e.getContent(), Overlay.ADDED);
		if (elem instanceof LibraryElement e
				&& e.getDatabase().isPresent()
				&& isNew(NavCache.get(e)))
			return Images.library(Overlay.ADDED);
		if (elem instanceof CategoryElement e
				&& isNew(NavCache.get(e)))
			return Images.get(e.getContent(), Overlay.ADDED);
		return null;
	}

	public static String getRepositoryText(DatabaseConfig dbConfig) {
		if (!Database.isActive(dbConfig) || !Repository.isConnected())
			return null;
		var repo = Repository.CURRENT;
		var ahead = repo.localHistory.getAheadOf(Constants.REMOTE_REF);
		var behind = repo.localHistory.getBehindOf(Constants.REMOTE_REF);
		var user = repo.user();
		var text = " [";
		if (!Strings.nullOrEmpty(user)) {
			text += user + "@";
		}
		if (repo.client != null) {
			text += repo.client.serverUrl + "/" + repo.client.repositoryId;
		} else {
			text += "local";
		}
		if (!ahead.isEmpty()) {
			text += " \u2191" + ahead.size();
		}
		if (!behind.isEmpty()) {
			text += " \u2193" + behind.size();
		}
		return text + "]";
	}

	public static String getStateIndicator(INavigationElement<?> elem) {
		if (Database.get() == null || !Repository.isConnected() || elem == null || elem.getLibrary().isPresent())
			return null;
		if (elem instanceof DatabaseElement e && !Database.isActive(e.getContent()))
			return null;
		if (elem instanceof LibraryDirElement e && elem.getParent() instanceof NavigationRoot)
			return null;
		if (elem instanceof LibraryElement e && e.getDatabase() == null)
			return null;
		if (!hasChanged(NavCache.get(elem)))
			return null;
		return CHANGED_STATE;
	}

	public static boolean hasChanged(INavigationElement<?> elem) {
		if (Database.get() == null || !Repository.isConnected() || elem == null || elem.getLibrary().isPresent())
			return false;
		return hasChanged(NavCache.get(elem));
	}

	public static boolean hasChanged(NavElement elem) {
		if (Database.get() == null || !Repository.isConnected() || elem == null || elem.isFromLibrary())
			return false;
		if (elem.is(ElementType.MODEL)) {
			if (isNew(elem))
				return false;
			var d = (RootDescriptor) elem.content();
			return !Repository.CURRENT.index.isSameVersion(getPath(d), d);
		}
		if (elem.is(ElementType.DATABASE) && librariesChanged())
			return true;
		if (elem.is(ElementType.LIBRARY_DIR))
			return librariesChanged();
		for (var child : elem.children())
			if (hasChanged(child) || (child.is(ElementType.MODEL, ElementType.CATEGORY) && isNew(child)))
				return true;
		return containsDeleted(elem);
	}

	private static boolean isNew(NavElement elem) {
		if (elem == null || elem.isFromLibrary())
			return false;
		if (elem.is(ElementType.LIBRARY) && isNewLibrary((String) elem.content()))
			return true;
		if (elem.is(ElementType.CATEGORY) && !Repository.CURRENT.index.contains(getPath(elem.content())))
			return true;
		if (elem.is(ElementType.MODEL) && Repository.CURRENT.index.getPath(elem.getTypedRefId()) == null)
			return true;
		return false;
	}

	private static boolean containsDeleted(NavElement elem) {
		if (elem.is(ElementType.MODEL))
			return false;
		for (var child : elem.children())
			if (containsDeleted(child))
				return true;
		if (!elem.is(ElementType.MODEL_TYPE, ElementType.CATEGORY))
			return false;
		var path = getPath(elem.content());
		var fromIndex = Repository.CURRENT.index.getSubPaths(path);
		var fromNavigation = elem.children()
				.stream().map(e -> getPath(e.content()))
				.collect(Collectors.toSet());
		for (var entry : fromIndex)
			if (!fromNavigation.contains(entry))
				return true;
		return false;
	}

	private static boolean librariesChanged() {
		var info = Repository.CURRENT.getInfo();
		var libsBefore = info == null ? new ArrayList<LibraryLink>() : info.libraries();
		var libsNow = LibraryLink.of(Database.get().getLibraries());
		if (libsBefore.size() != libsNow.size())
			return true;
		for (var lib : libsBefore)
			if (!libsNow.contains(lib))
				return true;
		for (var lib : libsNow)
			if (!libsBefore.contains(lib))
				return true;
		return false;
	}

	private static boolean isNewLibrary(String lib) {
		var info = Repository.CURRENT.getInfo();
		var libsBefore = info == null ? new ArrayList<LibraryLink>() : info.libraries();
		return !libsBefore.contains(new LibraryLink(lib, null));
	}

	private static String getPath(Object o) {
		if (o instanceof ModelType t)
			return Path.of(t);
		if (o instanceof Category c)
			return Path.of(c);
		if (o instanceof RootDescriptor d)
			return Path.of(Repository.CURRENT.descriptors.categoryPaths, d);
		return null;
	}

}
