package org.openlca.app.results.simulation;

import org.openlca.app.App;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
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
		try {
			Simulator solver = new Simulator(setup, matrixCache, App.getSolver());
			// do a first calculation that initialises the result;
			solver.nextRun();
			String setupKey = Cache.getAppCache().put(setup);
			String solverKey = Cache.getAppCache().put(solver);
			SimulationInput input = new SimulationInput(setupKey, solverKey);
			Editors.open(input, SimulationEditor.ID);
		} catch (Exception e) {
			log.error("Simulation initialisation failed", e);
		}
	}

}
