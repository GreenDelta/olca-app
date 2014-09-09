package org.openlca.app.results.simulation;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;

/**
 * The editor input for the Monte-Carlo-Simulation.
 */
class SimulationInput implements IEditorInput {

	private String setupKey;
	private String solverKey;

	public SimulationInput(String setupKey, String solverKey) {
		this.setupKey = setupKey;
		this.solverKey = solverKey;
	}

	public String getSetupKey() {
		return setupKey;
	}

	public String getSolverKey() {
		return solverKey;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.SIMULATE_16.getDescriptor();
	}

	@Override
	public String getName() {
		return Messages.MonteCarloSimulation;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getName();
	}

}
