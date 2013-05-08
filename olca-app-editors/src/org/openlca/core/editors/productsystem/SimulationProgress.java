package org.openlca.core.editors.productsystem;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.openlca.core.math.SimulationSolver;

/**
 * The progress that runs the simulations. This should not be executed in the
 * UI-thread.
 */
class SimulationProgress implements IRunnableWithProgress {

	private Display display;
	private int numberOfRuns;
	private SimulationSolver solver;
	private SimulationPage page;

	public SimulationProgress(Display display, SimulationInput input,
			SimulationPage page) {
		this.display = display;
		this.solver = input.getSolver();
		this.numberOfRuns = input.getNumberOfRuns();
		this.page = page;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask("Simulation...", numberOfRuns);
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
