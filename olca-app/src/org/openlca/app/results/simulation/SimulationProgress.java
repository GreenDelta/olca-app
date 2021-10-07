package org.openlca.app.results.simulation;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.M;
import org.openlca.core.math.Simulator;

/**
 * The progress that runs the simulations. This should not be executed in the
 * UI-thread.
 */
class SimulationProgress implements IRunnableWithProgress {

	private final Display display;
	private final int numberOfRuns;
	private final Simulator solver;
	private final SimulationPage page;

	public SimulationProgress(Display display, SimulationEditor editor,
			SimulationPage page) {
		this.display = display;
		this.solver = editor.simulator;
		this.numberOfRuns = editor.setup.numberOfRuns();
		this.page = page;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask(M.MonteCarloSimulation + "...", numberOfRuns);
		for (int i = 0; i < numberOfRuns; i++) {
			if (monitor.isCanceled()) {
				display.asyncExec(page::progressDone);
				break;
			}
			solver.nextRun();
			display.asyncExec(page::updateProgress);
			Thread.sleep(10);
		}
		monitor.done();
		display.asyncExec(page::progressDone);
	}
}
