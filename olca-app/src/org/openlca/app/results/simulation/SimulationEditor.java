package org.openlca.app.results.simulation;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.Simulator;
import org.openlca.util.Strings;

public class SimulationEditor extends SimpleFormEditor {

	public static String ID = "SimulationEditor";

	private CalculationSetup setup;
	private Simulator simulator;

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		super.init(site, editorInput);
		SimulationInput input = (SimulationInput) editorInput;
		setup = Cache.getAppCache().remove(input.getSetupKey(),
				CalculationSetup.class);
		setPartName(Strings.cut(setup.productSystem.name, 75));
		simulator = Cache.getAppCache()
				.remove(input.getSolverKey(), Simulator.class);
	}

	public CalculationSetup getSetup() {
		return setup;
	}

	public Simulator getSimulator() {
		return simulator;
	}

	@Override
	protected FormPage getPage() {
		return new SimulationPage(this);
	}

}
