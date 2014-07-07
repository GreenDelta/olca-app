package org.openlca.app.results.simulation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Editors;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.Simulator;
import org.openlca.core.matrix.cache.MatrixCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initialises the Monte Carlo simulation of a product system and opens the
 * editor.
 */
public class SimulationInit {

	private Logger log = LoggerFactory.getLogger(getClass());
	private CalculationSetup setup;
	private MatrixCache matrixCache;

	public SimulationInit(CalculationSetup setup, MatrixCache matrixCache) {
		this.setup = setup;
		this.matrixCache = matrixCache;
	}

	public void run() {
		InitJob job = new InitJob();
		job.setUser(true);
		job.schedule();
	}

	private class InitJob extends Job {

		public InitJob() {
			super(Messages.InitializeSimulation);
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.InitializeSimulation, IProgressMonitor.UNKNOWN);
			try {
				Simulator solver = new Simulator(setup, matrixCache,
						App.getSolver());
				// do a first calculation that initialises the result;
				solver.nextRun();
				String setupKey = Cache.getAppCache().put(setup);
				String solverKey = Cache.getAppCache().put(solver);
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
