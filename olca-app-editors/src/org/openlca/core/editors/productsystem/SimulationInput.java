package org.openlca.core.editors.productsystem;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.SimulationSolver;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.resources.ImageType;

public class SimulationInput implements IEditorInput {

	private SimulationSolver solver;
	private int numberOfRuns;
	private ProductSystem system;
	private IDatabase database;

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
		if (system != null && system.getName() != null)
			return system.getName();
		return "Simulation";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return "Simulation";
	}

	public ProductSystem getSystem() {
		return system;
	}

	public int getNumberOfRuns() {
		return numberOfRuns;
	}

	public SimulationSolver getSolver() {
		return solver;
	}

	public String getReferenceProcessName() {
		if (system == null || system.getReferenceProcess() == null)
			return null;
		return system.getReferenceProcess().getName();
	}

	public String getQuantitativeReference() {
		if (system == null || system.getReferenceExchange() == null)
			return null;
		Exchange refExchange = system.getReferenceExchange();
		if (refExchange.getFlow() == null || refExchange.getUnit() == null)
			return null;
		return refExchange.getResultingAmount().getValue() + "  "
				+ refExchange.getUnit().getName() + "  "
				+ refExchange.getFlow().getName();
	}

	public void setSolver(SimulationSolver solver) {
		this.solver = solver;
	}

	public void setNumberOfRuns(int numberOfRuns) {
		this.numberOfRuns = numberOfRuns;
	}

	public void setSystem(ProductSystem system) {
		this.system = system;
	}

	public IDatabase getDatabase() {
		return database;
	}

	public void setDatabase(IDatabase database) {
		this.database = database;
	}

}
