package org.openlca.app.simulation;

import org.eclipse.jface.operation.ModalContext;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper for a button that controls the simulation progress: start / cancel.
 */
class SimulationControl implements SelectionListener {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Button button;
	private SimulationMonitor monitor;
	private SimulationPage page;
	private SimulationEditor editor;

	public SimulationControl(Button button, SimulationEditor editor,
			SimulationPage page) {
		this.button = button;
		this.page = page;
		this.editor = editor;
		monitor = new SimulationMonitor();
		button.addSelectionListener(this);
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		widgetDefaultSelected(e);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		if (!monitor.isRunning()) {
			button.setText("Cancel");
			startProgress();
		} else {
			monitor.setCanceled(true);
		}
	}

	private void startProgress() {
		try {
			Display display = Display.getCurrent();
			SimulationProgress progress = new SimulationProgress(display,
					editor, page);
			ModalContext.run(progress, true, monitor, display);
		} catch (Exception e) {
			log.error("Could not start simulation progress", e);
		}
	}
}
