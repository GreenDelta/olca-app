package org.openlca.app.navigation.actions.libraries;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.MountLibraryDialog;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.library.Library;
import org.openlca.core.library.PreMountCheck;
import org.openlca.core.library.Unmounter;
import org.openlca.core.library.reader.LibReader;

class LibraryActions {

	static void mount(Library lib) {
		var res = App.exec(M.CheckLibraryDots,
				() -> PreMountCheck.check(Database.get(), lib));
		if (res == null) {
			ErrorReporter.on(
				"Mounting check returned no result for library"
				, "Library: " + lib);
			return;
		}
		if (res.isError()) {
			ErrorReporter.on("Failed to check library", res.error());
			return;
		}
		var mountRes = MountLibraryDialog.show(lib, res);
		if (mountRes.isError()) {
			ErrorReporter.on("Failed to mount library", mountRes.error());
		}
	}

	/// Removes the given library from the database. The user is asked for
	/// confirmation first.
	static void unmount(Library lib, Runnable callback) {
		execUnmount(lib, true, callback);
	}

	/// Removes the given library from the database as part of replacing it
	/// with another library. In this case the user is not asked for an extra
	/// confirmation when the library can only be completely removed, as the
	/// replacement was already confirmed.
	static void replace(Library lib, Runnable callback) {
		execUnmount(lib, false, callback);
	}

	private static void execUnmount(
		Library lib, boolean confirmRemoval, Runnable callback
	) {
		if (lib == null)
			return;
		var db = Database.get();
		if (db == null)
			return;

		// check if the library can be removed and which options are possible
		var options = App.exec(M.CheckLibraryDots,
			() -> UnmounterOptionCheck.run(lib, db));
		if (options.isEmpty())
			return; // the user was informed about the problem already
		var retentions = options.get();

		// when the library can only be completely removed, we may need to
		// ask the user for confirmation
		if (retentions.size() == 1
			&& retentions.contains(Unmounter.Retention.KEEP_NONE)) {
			if (confirmRemoval && !Question.ask(
				"Remove library from database",
				"Do you want to remove the library from the database?"))
				return;
			runUnmount(lib, Unmounter.Retention.KEEP_NONE, null, callback);
			return;
		}

		// otherwise, let the user select one of the options
		var selected = UnmounterOptionsDialog.show(retentions);
		if (selected.isEmpty())
			return;
		var retention = selected.get();

		// the keep options need a library reader
		LibReader reader = null;
		if (retention != Unmounter.Retention.KEEP_NONE) {
			reader = Libraries.readerOf(lib).orElse(null);
			if (reader == null) {
				MsgBox.error("Cannot remove library from database",
					"The library reader could not be created.");
				return;
			}
		}

		runUnmount(lib, retention, reader, callback);
	}

	private static void runUnmount(
		Library lib, Unmounter.Retention retention,
		LibReader reader, Runnable callback
	) {
		var db = Database.get();
		var libName = lib.name();
		Runnable action = switch (retention) {
			case KEEP_NONE -> () -> Unmounter.keepNone(db, libName);
			case KEEP_USED -> () -> Unmounter.keepUsed(db, reader);
			case KEEP_ALL -> () -> Unmounter.keepAll(db, reader);
		};
		App.exec(M.RemovingLibraryDots, action, callback);
	}

}
