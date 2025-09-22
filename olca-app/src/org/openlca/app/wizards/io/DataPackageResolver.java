package org.openlca.app.wizards.io;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.components.MountLibraryDialog;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.rcp.Workspace;
import org.openlca.core.database.DataPackage;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryDir;
import org.openlca.core.library.PreMountCheck;

class DataPackageResolver {

	private final LibraryDir libDir = Workspace.getLibraryDir();
	private final IDatabase database = Database.get();
	private final LinkedList<DataPackage> dataPackages;
	private final Set<String> handled = new HashSet<>();
	private final Consumer<Boolean> callback;

	private DataPackageResolver(Collection<DataPackage> dataPackages, Consumer<Boolean> callback) {
		this.dataPackages = new LinkedList<>(dataPackages);
		this.callback = callback;
	}

	static void resolve(Collection<DataPackage> dataPackages, Consumer<Boolean> callback) {
		if (dataPackages.isEmpty()) {
			callback.accept(true);
			return;
		}
		new DataPackageResolver(dataPackages, callback).next();
	}

	private void next() {
		if (dataPackages.isEmpty()) {
			callback.accept(true);
			return;
		}
		var next = dataPackages.pop();
		if (!next.isLibrary()) {
			database.addRepository(next.name(), next.version(), next.url());
			Repository.open(database, next);
			next();
		} else {
			var lib = libDir.getLibrary(next.name()).orElse(null);
			if (lib == null) {
				askFor(next);
			} else if (!handled.contains(lib.name())) {
				mount(lib);
			} else if (dataPackages.isEmpty()) {
				callback.accept(true);
			} else {
				next();
			}
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
