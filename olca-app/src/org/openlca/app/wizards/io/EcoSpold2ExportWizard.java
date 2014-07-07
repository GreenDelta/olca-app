package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.ecospold2.output.EcoSpold2Export;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EcoSpold2ExportWizard extends Wizard implements IExportWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ModelSelectionPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.EcoSpold2Export);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		File targetDir = page.getExportDestination();
		List<BaseDescriptor> selection = page.getSelectedModels();
		List<ProcessDescriptor> processes = new ArrayList<>();
		for (BaseDescriptor descriptor : selection) {
			if (descriptor instanceof ProcessDescriptor)
				processes.add((ProcessDescriptor) descriptor);
		}
		EcoSpold2Export export = new EcoSpold2Export(targetDir, Database.get(),
				processes);
		runExport(export);
		return true;
	}

	private void runExport(final EcoSpold2Export export) {
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					export.run();
				}
			});
		} catch (Exception e) {
			log.error("Export failed", e);
		}
	}

	@Override
	public void addPages() {
		page = new ModelSelectionPage(ModelType.PROCESS);
		addPage(page);
	}

}
