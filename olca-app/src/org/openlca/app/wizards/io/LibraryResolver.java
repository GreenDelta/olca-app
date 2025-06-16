package org.openlca.app.wizards.io;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.MountLibraryDialog;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.rcp.Workspace;
import org.openlca.core.database.DataPackage;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryDir;
import org.openlca.core.library.PreMountCheck;

class LibraryResolver {

	private final LibraryDir libDir = Workspace.getLibraryDir();
	private final IDatabase database = Database.get();
	private final LinkedList<DataPackage> libraries;
	private final Set<String> handled = new HashSet<>();
	private final Consumer<Boolean> callback;

	private LibraryResolver(List<DataPackage> libraries, Consumer<Boolean> callback) {
		this.libraries = new LinkedList<>(libraries);
		this.callback = callback;
	}

	static void resolve(Set<DataPackage> packages, Consumer<Boolean> callback) {
		var libraries = packages.stream()
				.filter(DataPackage::isLibrary)
				.collect(Collectors.toList());
		if (libraries.isEmpty()) {
			callback.accept(true);
			return;
		}
		new LibraryResolver(libraries, callback).next();
	}

	private void next() {
		if (libraries.isEmpty()) {
			callback.accept(true);
			return;
		}
		var next = libraries.pop();
		var lib = libDir.getLibrary(next.name()).orElse(null);
		if (lib == null) {
			askFor(next);
		} else if (!handled.contains(lib.name())) {
			mount(lib);
		} else if (libraries.isEmpty()) {
			callback.accept(true);
		} else {
			next();
		}
	}

	private void askFor(DataPackage library) {
		var dialog = new LibraryDialog(library);
		if (dialog.open() != LibraryDialog.OK) {
			callback.accept(false);
			return;
		}
		var resolved = dialog.isFileSelected()
				? App.exec(M.ExtractingLibrary + " - " + library.name(),
						() -> Libraries.importFromFile(new File(dialog.getLocation())))
				: App.exec(M.DownloadingAndExtractingLibrary + " - " + library.name(),
						() -> Libraries.importFromUrl(dialog.getLocation()));
		if (resolved == null) {
			askFor(library);
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
			next();
		});
	}

}
