package org.openlca.app.editors.systems;

import org.openlca.app.App;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.util.Question;
import org.openlca.app.wizards.calculation.CalculationWizard;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.ProductSystems;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;

class CalculationDispatch {

	private final ProductSystem system;

	private CalculationDispatch(ProductSystem system) {
		this.system = system;
	}

	static void call(ProductSystem system) {
		if (system == null)
			return;
		new CalculationDispatch(system).call();
	}

	private void call() {
		if (shouldCheckSystem()) {
			LoggerFactory.getLogger(getClass())
					.info("check if system {} is linked", system);
			boolean linked = App.exec(
					"Check product system ...",
					() -> ProductSystems.isConnected(system));
			updateMarker(linked);
			if (!linked) {
				var b = Question.ask(
						"Graph not fully connected",
						"Calculate results anyway?");
				if (!b)
					return;
			}
		}
		CalculationWizard.open(system);
	}

	private boolean shouldCheckSystem() {
		// we should check the system when there is an _unlined marker file,
		// when there was no calculation yet, or when the system was modified
		// since the last calculation
		if (unlinkedMarker().exists())
			return true;
		var dir = DatabaseDir.getDir(system);
		var prefs = new File(dir, "calculation-preferences.json");
		return !prefs.exists() || system.lastChange >= prefs.lastModified();
	}

	private File unlinkedMarker() {
		var dir = DatabaseDir.getDir(system);
		return new File(dir, "_unlinked");
	}

	private void updateMarker(boolean linked) {
		try {
			var marker = unlinkedMarker();
			if (linked && marker.exists()) {
				Files.delete(marker.toPath());
			}	else if (!linked && !marker.exists()) {
				Files.createFile(marker.toPath());
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass())
					.error("failed to update _unlinked marker", e);
		}
	}
}
