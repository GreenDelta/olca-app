package org.openlca.app.results.simulation;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;

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
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.SIMULATE.descriptor();
	}

	@Override
	public String getName() {
		return M.MonteCarloSimulation;
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
