package org.openlca.app.results.simulation;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.Messages;
import org.openlca.core.math.Simulator;

/**
 * The progress that runs the simulations. This should not be executed in the
 * UI-thread.
 */
class SimulationProgress implements IRunnableWithProgress {

	private Display display;
	private int numberOfRuns;
	private Simulator solver;
	private SimulationPage page;

	public SimulationProgress(Display display, SimulationEditor editor,
			SimulationPage page) {
		this.display = display;
		this.solver = editor.getSimulator();
		this.numberOfRuns = editor.getSetup().getNumberOfRuns();
		this.page = page;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask(Messages.MonteCarloSimulation + "...", numberOfRuns);
		for (int i = 0; i < numberOfRuns; i++) {
			if (monitor.isCanceled()) {
				doneAfter(i);
				break;
			}
			doNextRun();
		}
		monitor.done();
		doneAfter(numberOfRuns);
	}

	private void doneAfter(final int numberOfRuns) {
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				page.progressDone(numberOfRuns);
			}
		});
	}

	private void doNextRun() throws InterruptedException {
		solver.nextRun();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				page.updateProgress();
			}
		});
		Thread.sleep(10);
	}
}
