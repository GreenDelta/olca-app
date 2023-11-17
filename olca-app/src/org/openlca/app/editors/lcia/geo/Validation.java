package org.openlca.app.editors.lcia.geo;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.geo.calc.FeatureValidation;
import org.openlca.geo.lcia.GeoFactorSetup;

class Validation {

	private Validation() {
	}

	static void run(GeoFactorSetup setup) {
		if (setup == null
				|| setup.features == null
				|| setup.features.isEmpty()) {
			MsgBox.error("Invalid setup",
					"Could not find any geometry in the setup.");
			return;
		}

		var validation = FeatureValidation.of(setup.features);
		Runnable onFinished = () -> {
			System.out.println(validation.stats().toString());
		};

		var job = new Job("Validate geometries") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Validate geometries", validation.count());
				validation.onValidated((count) -> {
					monitor.worked(count);
					if (monitor.isCanceled()) {
						validation.cancel();
					}
				});
				validation.run();
				// TODO: return CANCEL when validation was cancelled
				onFinished.run();
				return Status.OK_STATUS;
			}
		};
		PlatformUI.getWorkbench()
				.getProgressService()
				.showInDialog(UI.shell(), job);
		job.schedule();
	}

}
