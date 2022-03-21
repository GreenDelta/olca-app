package org.openlca.app.navigation.actions.libraries;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.DataDir;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.library.Library;
import org.openlca.util.Dirs;
import org.slf4j.LoggerFactory;

public class DeleteLibraryAction extends Action implements INavigationAction {

	private LibraryElement element;

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (first instanceof LibraryElement) {
			this.element = (LibraryElement) first;
			return true;
		}
		return false;
	}

	@Override
	public String getText() {
		return "Remove library";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.DELETE.descriptor();
	}

	@Override
	public void run() {
		if (element == null)
			return;
		var lib = element.getContent();
		if (lib == null)
			return;

		// check if this is a mounted library
		var db = element.getDatabase();
		if (db.isPresent()) {
			// TODO: unmount a library from a database
			MsgBox.info("Not yet supported",
				"Removing a library from a database is not yet supported.");
			return;
		}

		// ask and delete the library
		boolean b = Question.ask("Delete library?",
			"Do you really want to delete the library? " +
			"Make sure that you have a backup of it.");
		if (!b)
			return;

		// check that it is not used
		var config = App.exec(
			"Check if library is used ...",
			() -> isUsed(lib));
		if (config.isPresent()) {
			MsgBox.info("Cannot delete library",
				"We cannot delete library " + lib.id() +
				" as it is still used in database " +
				config.get().name() + ".");
			return;
		}

		// delete it
		Dirs.delete(lib.folder());
		Navigator.refresh();
	}

	/**
	 * Returns the first database where the given library is used.
	 * An empty option is returned if the library is not used in
	 * any of the databases in the database folder.
	 */
	private Optional<DatabaseConfig> isUsed(Library lib) {
		if (lib == null)
			return Optional.empty();
		var log = LoggerFactory.getLogger(getClass());
		var libID = lib.id();

		Predicate<DatabaseConfig> isUsedIn = config -> {
			if (config == null)
				return false;
			log.info("Check usage of {} in {}", libID, config.name());
			if (Database.isActive(config)) {
				var db = Database.get();
				return db.getLibraries().contains(libID);
			}
			try (var db = config.connect(DataDir.databases())) {
				return db.getVersion() >= 10
					&& db.getLibraries().contains(libID);
			} catch (Exception e) {
				throw new RuntimeException(
					"Failed to check library usage in database " + config.name());
			}
		};

		var configs = Database.getConfigurations();
		return Stream.concat(
			configs.getDerbyConfigs().stream(),
			configs.getMySqlConfigs().stream())
			.parallel()
			.filter(isUsedIn)
			.findAny();
	}
}
