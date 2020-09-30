package org.openlca.app.preferences;

import java.util.concurrent.atomic.AtomicBoolean;

import org.openlca.app.App;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.julia.Julia;

public class LibraryDownload {

	public static void open() {
		boolean b = Question.ask("Download calculation libraries",
				"You can download additional libraries to make"
						+ " the calculation faster. However, this currently"
						+ " only improves the calculation speed of product"
						+ " systems with sparse matrices in the quick"
						+ " calculation. Also, some of these libraries are"
						+ " licensed under the GNU General Public License v2"
						+ " (see https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)."
						+ " By downloading these libraries you accept the terms and"
						+ " conditions of this license. Do you want to download"
						+ " these additional calculation libraries?");
		if (!b)
			return;

		var success = new AtomicBoolean(false);
		App.runWithProgress("Download native libraries", () -> {
			var result = Julia.fetchSparseLibraries();
			success.set(result);
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
