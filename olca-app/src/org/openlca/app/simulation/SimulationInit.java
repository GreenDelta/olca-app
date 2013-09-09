package org.openlca.app.simulation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.openlca.app.App;
import org.openlca.app.util.Editors;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.Simulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the Monte Carlo simulation of a product system and opens the
 * editor.
 */
public class SimulationInit {

	private Logger log = LoggerFactory.getLogger(getClass());
	private CalculationSetup setup;
	private IDatabase database;

	public SimulationInit(CalculationSetup setup, IDatabase database) {
		this.setup = setup;
		this.database = database;
	}

	public void run() {
		InitJob job = new InitJob();
		job.setUser(true);
		job.schedule();
	}

	private class InitJob extends Job {

		public InitJob() {
			super("Initialize simulation");
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Initialize simulation", IProgressMonitor.UNKNOWN);
			try {
				Simulator solver = new Simulator(setup, database);
				// do a first calculation that initialises the result;
				solver.nextRun();
				String setupKey = App.getCache().put(setup);
				String solverKey = App.getCache().put(solver);
				SimulationInput input = new SimulationInput(setupKey, solverKey);
				Editors.open(input, SimulationEditor.ID);
				monitor.done();
				return Status.OK_STATUS;
			} catch (Exception e) {
				log.error("Simulation initialisation failed", e);
				return Status.CANCEL_STATUS;
			}
		}
	}

}
