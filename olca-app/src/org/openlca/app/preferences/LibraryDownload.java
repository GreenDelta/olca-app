package org.openlca.app.preferences;

import java.util.concurrent.atomic.AtomicBoolean;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.preferences.LibDownload.Repo;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.nativelib.Module;
import org.openlca.nativelib.NativeLib;
import org.slf4j.LoggerFactory;

public class LibraryDownload {

	public static void open() {
		boolean b = Question.ask(M.DownloadCalculationLibraries,
				M.DownloadCalculationLibrariesQuestion
						+ "\r\n[1] "
						+ "https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html");
		if (!b)
			return;

		var success = new AtomicBoolean(false);
		App.runWithProgress("Download native libraries", () -> {
			try {
				LibDownload.fetch(Repo.GITHUB, Module.UMFPACK, Workspace.root());
				NativeLib.reloadFrom(Workspace.root());
				success.set(true);
			} catch (Exception e) {
				var log = LoggerFactory.getLogger(LibraryDownload.class);
				log.error("failed to download native libraries", e);
			}
		}, () -> {
			if (!success.get()) {
				MsgBox.error("The download of the libraries or "
						+ "loading them failed. Please check the "
						+ "log file for details.");
			} else {
				MsgBox.info("Download finished", "Note that you need to "
						+ "restart openLCA in order to use "
						+ "the downloaded libraries.");
			}
		});
	}
}
