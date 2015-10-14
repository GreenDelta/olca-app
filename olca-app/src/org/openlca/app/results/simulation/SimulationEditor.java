package org.openlca.app.results.simulation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Cache;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.Simulator;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulationEditor extends FormEditor {

	public static String ID = "SimulationEditor";

	private Logger log = LoggerFactory.getLogger(getClass());

	private CalculationSetup setup;
	private Simulator simulator;

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		super.init(site, editorInput);
		SimulationInput input = (SimulationInput) editorInput;
		setup = Cache.getAppCache().remove(input.getSetupKey(),
				CalculationSetup.class);
		setPartName(Strings.cut(setup.productSystem.getName(), 75));
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
	protected void addPages() {
		try {
			addPage(new SimulationPage(this));
		} catch (Exception e) {
			log.error("Failed to add simulation page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
}
