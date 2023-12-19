package org.openlca.app.editors.lcia.geo;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.App;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.geo.calc.FeatureRepair;
import org.openlca.geo.lcia.GeoFactorSetup;

class Repair {

	private Repair() {
	}

	static void run(GeoFactorSetup setup) {
		if (setup == null
				|| setup.features == null
				|| setup.features.isEmpty()) {
			MsgBox.error("Invalid setup",
					"Could not find any geometry in the setup.");
			return;
		}

		var repair = FeatureRepair.of(setup.features);
		var job = new UIJob("Repair features") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				monitor.beginTask("Repair features", repair.count());
				repair.onHandled((count) -> {
					monitor.worked(count);
					if (monitor.isCanceled()) {
						repair.cancel();
					}
				});
				repair.run();
				monitor.done();
				showInfo(repair);
				return repair.wasCanceled()
						? Status.CANCEL_STATUS
						: Status.OK_STATUS;
			}
		};
		PlatformUI.getWorkbench()
				.getProgressService()
				.showInDialog(UI.shell(), job);
		job.schedule();
	}

	private static void showInfo(FeatureRepair repair) {
		if (repair.wasCanceled())
			return;
		App.runInUI("Feature repair done", () -> {
			var msg = repair.count() == 1
					? "Checked one feature."
					: "Checked " + repair.count() + " features.";
			MsgBox.info("Feature repair done", msg);
		});
	}

}
