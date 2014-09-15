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
import org.openlca.io.xls.process.output.ExcelExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelExportWizard extends Wizard implements IExportWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ModelSelectionPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.ExcelExport);
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
		List<ProcessDescriptor> processes = new ArrayList<>();
		for (BaseDescriptor descriptor : page.getSelectedModels()) {
			if (descriptor instanceof ProcessDescriptor)
				processes.add((ProcessDescriptor) descriptor);
		}
		ExcelExport export = new ExcelExport(dir, Database.get(), processes);
		run(export);
		return true;
	}

	private void run(final ExcelExport export) {
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.Export,
							IProgressMonitor.UNKNOWN);
					export.run();
					monitor.done();
				}
			});
		} catch (Exception e) {
			log.error("Export failed", e);
		}
	}
}
