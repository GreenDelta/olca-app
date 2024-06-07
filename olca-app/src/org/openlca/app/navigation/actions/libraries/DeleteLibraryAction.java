package org.openlca.app.navigation.actions.libraries;

import static org.openlca.app.licence.LibrarySession.removeSession;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.library.Library;
import org.openlca.core.library.Unmounter;
import org.openlca.util.Dirs;

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
		if (element != null && element.getDatabase().isEmpty())
			return M.RemoveLibrary;
		return M.RemoveLibraryExperimental;
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
			if (Question.ask(M.RemovingLibraryWarning,
					M.RemovingLibraryExplanations + "\r\n" + M.DoYouWantToContinue)) {
				App.runWithProgress(M.RemovingLibraryDots,
						() -> new Unmounter(db.get()).unmountUnsafe(lib.name()),
						Navigator::refresh);
			}
		} else {
			delete(lib);
		}
	}

	private void delete(Library lib) {
		// ask and delete the library
		boolean b = Question.ask(M.DeleteLibraryQ, M.DeleteLibraryQuestion);
		if (!b)
			return;

		// check that it is not used
		var usage = App.exec(
				M.CheckIfLibraryIsUsedDots,
				() -> Usage.find(lib));
		if (usage.isPresent()) {
			var u = usage.get();
			if (u.isError()) {
				ErrorReporter.on(
						"Failed to check usage of library '"
								+ lib.name() + "'",
						u.error);
				return;
			}
			MsgBox.info(M.CannotDeleteLibrary, M.CannotDeleteLibraryInfo
					+ "\r\n " + u.label());
			return;
		}

		// delete it
		Dirs.delete(lib.folder());
		removeSession(lib.name());
		Navigator.refresh();
	}

	private enum UsageType {
		DATABASE, LIBRARY
	}

	private record Usage(String name, UsageType type, String error) {

		private static Usage db(DatabaseConfig config) {
			return new Usage(config.name(), UsageType.DATABASE, null);
		}

		private static Usage lib(Library lib) {
			return new Usage(lib.name(), UsageType.LIBRARY, null);
		}

		static Optional<Usage> find(Library lib) {

			// first check in other libraries (this is fast)
			var usage = Workspace.getLibraryDir()
					.getLibraries()
					.stream()
					.filter(other -> !Objects.equals(other, lib))
					.map(other -> Usage.of(other, lib))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.findAny();
			if (usage.isPresent())
				return usage;

			// then check in databases
			var configs = Database.getConfigurations();
			return Stream.concat(
					configs.getDerbyConfigs().stream(),
					configs.getMySqlConfigs().stream())
					.parallel()
					.map(config -> Usage.of(config, lib))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.findAny();
		}

		static Optional<Usage> of(DatabaseConfig config, Library lib) {
			if (config == null)
				return Optional.empty();
			if (Database.isActive(config)) {
				var db = Database.get();
				return db.getLibraries().contains(lib.name())
						? Optional.of(Usage.db(config))
						: Optional.empty();
			}
			try (var db = config.connect(Workspace.dbDir())) {
				return db.getVersion() >= 10 && db.getLibraries().contains(lib.name())
						? Optional.of(Usage.db(config))
						: Optional.empty();
			} catch (Exception e) {
				var usage = new Usage(
						config.name(), UsageType.DATABASE, e.getMessage());
				return Optional.of(usage);
			}
		}

		static Optional<Usage> of(Library other, Library lib) {
			if (Objects.equals(other, lib))
				return Optional.empty();
			return other.getDirectDependencies().contains(lib)
					? Optional.of(Usage.lib(other))
					: Optional.empty();
		}

		boolean isError() {
			return error != null;
		}

		String label() {
			return type == UsageType.DATABASE
					? "database " + name
					: "library " + name;
		}
	}

}
