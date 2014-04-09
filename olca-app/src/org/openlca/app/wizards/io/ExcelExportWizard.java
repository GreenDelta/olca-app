package org.openlca.app.wizards.io;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ExcelExportWizard extends Wizard implements IExportWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ModelSelectionPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Excel Export");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = new ModelSelectionPage(ModelType.PROCESS);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		File dir = page.getExportDestination();
		for (BaseDescriptor descriptor : page.getSelectedModels()) {

		}
		return false;
	}
}
