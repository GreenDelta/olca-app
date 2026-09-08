package org.openlca.app.wizards.io;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.MountLibraryDialog;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.rcp.Workspace;
import org.openlca.commons.Res;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryDir;
import org.openlca.core.library.PreMountCheck;
import org.openlca.jsonld.LibraryLink;

@NullMarked
class LibraryResolver {

	private final IDatabase db;
	private final LibraryDir libDir = Workspace.getLibraryDir();

	private LibraryResolver(IDatabase db) {
		this.db = db;
	}

	static Res<Void> resolve(@Nullable List<LibraryLink> links) {
		if (links == null || links.isEmpty())
			return Res.ok();
		var db = Database.get();
		return db == null
			? Res.error("No active database found")
			: new LibraryResolver(db).resolveAll(links);
	}

	private Res<Void> resolveAll(List<LibraryLink> links) {

		var visited = new HashSet<>(db.getLibraries());

		for (var link : links) {
			if (visited.contains(link.id()))
				continue;

			// resolve the library
			var libRes = resolve(link);
			if (libRes.isError())
				return libRes.wrapError("Failed to resolve library");
			var lib = libRes.value();

			// try to mount it
			var res = PreMountCheck.check(db, lib);
			if (res.isError())
				return Res.error(res.error());

			var mounted = MountLibraryDialog.show(lib, res);
			if (mounted.isError())
				return mounted.wrapError("Failed to mount library");
			if (mounted.value().isEmpty())
				return Res.error("Required library was not mounted: " + lib);

			mounted.value().forEach(l -> visited.add(l.name()));
		}
		return Res.ok();
	}

	private Res<Library> resolve(LibraryLink link) {
		var lib = libDir.getLibrary(link.id()).orElse(null);
		if (lib != null)
			return Res.ok(lib);

		var dialog = new LibraryDialog(link);
		if (dialog.open() != LibraryDialog.OK) {
			// user canceled the dialog to add a library (?)
			return Res.error("Dialog was canceled.");
		}
		var resolved = dialog.isFileSelected()
			? App.exec(M.ExtractingLibrary + " - " + link.id(),
			() -> Libraries.importFromFile(new File(dialog.getLocation())))
			: App.exec(M.DownloadingAndExtractingLibrary + " - " + link.id(),
			() -> Libraries.importFromUrl(dialog.getLocation()));

		return resolved != null
			? Res.ok(resolved)
			: resolve(link);
	}
}
