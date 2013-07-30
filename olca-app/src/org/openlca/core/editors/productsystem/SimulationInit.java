package org.openlca.core.editors.productsystem;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.openlca.app.Editors;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.SimulationSolver;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the Monte Carlo simulation of a product system and opens the
 * editor.
 */
class SimulationInit {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ProductSystem system;
	private IDatabase database;
	private int iterationCount;
	private ImpactMethodDescriptor impactMethod;
	private AllocationMethod allocationMethod;

	public SimulationInit(ProductSystem system, IDatabase database,
			int iterationCount) {
		this.database = database;
		this.iterationCount = iterationCount;
		this.system = system;
	}

	public void setImpactMethod(ImpactMethodDescriptor impactMethod) {
		this.impactMethod = impactMethod;
	}

	public void setAllocationMethod(AllocationMethod allocationMethod) {
		this.allocationMethod = allocationMethod;
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
				SimulationSolver solver = setUpSolver();
				SimulationInput input = createInput(solver);
				Editors.open(input, SimulationEditor.ID);
				monitor.done();
				return Status.OK_STATUS;
			} catch (Exception e) {
				log.error("Simulation initialisation failed", e);
				return Status.CANCEL_STATUS;
			}
		}

		private SimulationSolver setUpSolver() throws Exception {
			SimulationSolver solver = new SimulationSolver(system, database);
			if (impactMethod == null)
				solver.setUp(allocationMethod);
			else
				solver.setUp(allocationMethod, impactMethod);
			return solver;
		}

		private SimulationInput createInput(SimulationSolver solver) {
			SimulationInput input = new SimulationInput();
			input.setDatabase(database);
			input.setNumberOfRuns(iterationCount);
			input.setSolver(solver);
			input.setSystem(system);
			return input;
		}
	}

}
