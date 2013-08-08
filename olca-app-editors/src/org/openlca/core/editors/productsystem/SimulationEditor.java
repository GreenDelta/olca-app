package org.openlca.core.editors.productsystem;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.core.database.IDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulationEditor extends FormEditor {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IDatabase database;
	public static String ID = "SimulationEditor";

	@Override
	protected void addPages() {
		try {
			addPage(new SimulationPage(this, ID, "Simulation", database));
		} catch (Exception e) {
			log.error("Failed to add simulation page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO: save simulation result after the calculation is finished
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		setPartName(input.getName());
		SimulationInput sInput = (SimulationInput) input;
		this.database = sInput.getDatabase();
	}

}
