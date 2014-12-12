package org.openlca.app.wizards.io;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.model.ModelType;

public class JsonExportWizard extends Wizard implements IExportWizard {

	private ModelSelectionPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("@Export data sets");
	}

	@Override
	public void addPages() {
		page = new ModelSelectionPage(ModelType.PROCESS);
	}

	@Override
	public boolean performFinish() {
		return false;
	}
}
