package org.openlca.app.collaboration.navigation;

import java.util.HashSet;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.navigation.NavElement.ElementType;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DataPackageElement;
import org.openlca.app.navigation.elements.DataPackagesElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.core.database.DataPackage;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Path;
import org.openlca.util.Strings;

public class RepositoryLabel {

	public static final String CHANGED_STATE = "> ";

	public static Image getWithOverlay(INavigationElement<?> elem) {
		if (Database.get() == null || Repository.get() == null)
			return null;
		if (elem instanceof ModelElement e
				&& !e.getDataPackage().isPresent()
				&& isNew(NavCache.get(e)))
			return Images.get(e.getContent(), Overlay.ADDED);
		if (elem instanceof DataPackageElement e
				&& e.getDatabase().isPresent()
				&& isNew(NavCache.get(e)))
			return Images.library(Overlay.ADDED);
		if (elem instanceof CategoryElement e
				&& isNew(NavCache.get(e)))
			return Images.get(e.getContent(), Overlay.ADDED);
		return null;
	}

	public static String getRepositoryText(Repository repo) {
		if (repo == null)
			return null;
		var ahead = repo.localHistory.getAheadOf(Constants.REMOTE_REF);
		var behind = repo.localHistory.getBehindOf(Constants.REMOTE_REF);
		var user = repo.user();
		var text = " [";
		if (!Strings.nullOrEmpty(user)) {
			text += user + "@";
		}
		text += repo.url;
		if (!ahead.isEmpty()) {
			text += " \u2191" + ahead.size();
		}
		if (!behind.isEmpty()) {
			text += " \u2193" + behind.size();
		}
		return text + "]";
	}

	public static String getStateIndicator(INavigationElement<?> elem) {
		var repo = Repository.get();
		if (Database.get() == null || repo == null || elem == null)
			return null;
		if (elem instanceof DatabaseElement e && !Database.isActive(e.getContent()))
			return null;
		if (elem instanceof DataPackagesElement && elem.getParent() instanceof NavigationRoot)
			return null;
		if (elem instanceof DataPackageElement e && e.getDatabase() == null)
			return null;
		if (!hasChanged(NavCache.get(elem)))
			return null;
		return CHANGED_STATE;
	}

	public static boolean hasChanged(INavigationElement<?> elem) {
		if (Database.get() == null || Repository.get() == null || elem == null)
			return false;
		return hasChanged(NavCache.get(elem));
	}

	private static boolean hasChanged(NavElement elem) {
		if (Database.get() == null || Repository.get() == null || elem == null || elem.isFromDataPackage())
			return false;
		if (elem.is(ElementType.MODEL)) {
			if (isNew(elem))
				return false;
			var d = (RootDescriptor) elem.content();
			var repo = Repository.get();
			return !repo.index.isSameVersion(getPath(d), d);
		}
		if (elem.is(ElementType.DATABASE) && dataPackagesChanged())
			return true;
		if (elem.is(ElementType.DATAPACKAGES))
			return dataPackagesChanged();
		if (elem.is(ElementType.DATAPACKAGE))
			return dataPackageChanged((DataPackage) elem.content());
		for (var child : elem.children())
			if (hasChanged(child) || (child.is(ElementType.MODEL, ElementType.CATEGORY) && isNew(child)))
				return true;
		return containsDeleted(elem);
	}

	private static boolean dataPackagesChanged() {
		var repo = Repository.get();
		var before = repo.getDataPackages();
		var now = Database.dataPackages().getAll();
		if (before.size() != now.size())
			return true;
		for (var lib : before)
			if (!now.contains(lib))
				return true;
		for (var lib : now) {
			if (!before.contains(lib))
				return true;
			var fromDb = before.stream().filter(n -> n.name().equals(lib.name())).findFirst().orElse(null);
			if (!fromDb.version().equals(lib.version()))
				return true;
		}
		return false;
	}

	private static boolean dataPackageChanged(DataPackage dataPackage) {
		var repo = Repository.get();
		var fromRepo = repo.getDataPackage(dataPackage.name());
		return fromRepo != null && !fromRepo.version().equals(dataPackage.version());
	}

	private static boolean isNew(NavElement elem) {
		if (elem == null || elem.isFromDataPackage())
			return false;
		if (elem.is(ElementType.DATAPACKAGE) && isNewDataPackage((DataPackage) elem.content()))
			return true;
		var repo = Repository.get();
		if (elem.is(ElementType.CATEGORY) && !repo.index.contains(getPath(elem.content())))
			return true;
		if (elem.is(ElementType.MODEL) && repo.index.getPath(elem.getTypedRefId()) == null)
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
		var repo = Repository.get();
		var fromIndex = repo.index.getSubPaths(path);
		var fromNavigation = elem.children()
				.stream().map(e -> getPath(e.content()))
				.collect(Collectors.toSet());
		for (var entry : fromIndex)
			if (!fromNavigation.contains(entry))
				return true;
		return false;
	}

	private static boolean isNewDataPackage(DataPackage dataPackage) {
		var info = Repository.get().getInfo();
		var before = info != null
				? info.dataPackages()
				: new HashSet<>();
		return !before.contains(dataPackage);
	}

	private static String getPath(Object o) {
		if (o instanceof ModelType t)
			return Path.of(t);
		if (o instanceof Category c)
			return Path.of(c);
		if (o instanceof RootDescriptor d)
			return Path.of(Repository.descriptors().categoryPaths, d);
		return null;
	}

}
