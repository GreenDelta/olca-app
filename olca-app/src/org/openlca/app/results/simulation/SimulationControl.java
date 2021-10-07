package org.openlca.app.results.simulation;

import org.eclipse.jface.operation.ModalContext;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper for a button that controls the simulation progress: start / cancel.
 */
class SimulationControl {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final SimulationMonitor monitor;
	private final SimulationPage page;
	private final SimulationEditor editor;

	public SimulationControl(Button button, SimulationEditor editor,
			SimulationPage page) {
		this.page = page;
		this.editor = editor;
		monitor = new SimulationMonitor();
		Controls.onSelect(button, (e) -> {
			if (!monitor.isRunning()) {
				button.setText(M.Cancel);
				startProgress();
			} else {
				monitor.setCanceled(true);
			}
		});
	}

	private void startProgress() {
		try {
			var display = Display.getCurrent();
			var progress = new SimulationProgress(display, editor, page);
			ModalContext.run(progress, true, monitor, display);
		} catch (Exception e) {
			log.error("Could not start simulation progress", e);
		}
	}
}
