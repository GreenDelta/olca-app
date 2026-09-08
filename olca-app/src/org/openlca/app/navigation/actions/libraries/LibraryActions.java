package org.openlca.app.navigation.actions.libraries;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.MountLibraryDialog;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Question;
import org.openlca.core.library.Library;
import org.openlca.core.library.PreMountCheck;
import org.openlca.core.library.Unmounter;
import org.openlca.license.License;

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

	static void unmount(Library lib, Runnable callback) {
		var license = License.of(lib.folder());
		Runnable action = () -> Unmounter.keepNone(Database.get(), lib.name());
		if (license.isEmpty()) {
			var reader = Libraries.readerOf(lib);
			if (reader.isEmpty())
				return;
			var answer = Question.ask(M.UnmountTitle, M.UnmountQuestion,
					new String[] { M.Cancel, M.KeepAll, M.KeepUsed, M.DeleteAll });
			if (answer == 0)
				return;
			if (answer == 1) {
				action = () -> Unmounter.keepAll(Database.get(), reader.get());
			} else if (answer == 2) {
				action = () -> Unmounter.keepUsed(Database.get(), reader.get());
			}
		}
		App.exec(M.RemovingLibraryDots, action, callback);
	}

}
