package org.openlca.app.wizards.io;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.openlca.app.App;
import org.openlca.app.components.MountLibraryDialog;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.rcp.Workspace;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryDir;
import org.openlca.core.library.PreMountCheck;
import org.openlca.jsonld.LibraryLink;

class LibraryResolver {

	private final LibraryDir libDir = Workspace.getLibraryDir();
	private final IDatabase database = Database.get();
	private final LinkedList<LibraryLink> links = new LinkedList<>();
	private final Set<String> handled = new HashSet<>();
	private final Consumer<Boolean> callback;

	private LibraryResolver(List<LibraryLink> links, Consumer<Boolean> callback) {
		this.links.addAll(links);
		this.callback = callback;
	}

	static void resolve(List<LibraryLink> links, Consumer<Boolean> callback) {
		if (links.isEmpty()) {
			callback.accept(true);
		}
		new LibraryResolver(links, callback).next();
	}

	private void next() {
		var link = links.pop();
		var lib = libDir.getLibrary(link.id()).orElse(null);
		if (lib == null) {
			askFor(link);
		} else if (!handled.contains(lib.name())) {
			mount(lib);
		} else if (links.isEmpty()) {
			callback.accept(true);
		} else {
			next();
		}
	}

	private void askFor(LibraryLink link) {
		var dialog = new LibraryDialog(link);
		if (dialog.open() != LibraryDialog.OK) {
			callback.accept(false);
			return;
		}
		Library resolved = dialog.isFileSelected()
				? App.exec("Extracting library " + link.id(),
						() -> Libraries.importFromFile(new File(dialog.getLocation())))
				: App.exec("Downloading and extracting library " + link.id(),
						() -> Libraries.importFromUrl(dialog.getLocation()));
		if (resolved == null) {
			askFor(link);
		} else {
			mount(resolved);
		}
	}

	private void mount(Library lib) {
		var result = PreMountCheck.check(database, lib);
		MountLibraryDialog.show(lib, result, success -> {
			if (success.isEmpty()) {
				callback.accept(false);
				return;
			}
			success.stream().map(Library::name).forEach(handled::add);
			if (links.isEmpty()) {
				callback.accept(true);
				return;
			}
			next();
		});
	}

}
