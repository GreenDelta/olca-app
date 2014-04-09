package org.openlca.app.wizards.io;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.io.ecospold1.exporter.EcoSpold01Outputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for exporting processes and impact methods to the EcoSpold01 format
 */
public class EcoSpold01ExportWizard extends Wizard implements IExportWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private ModelSelectionPage page;
	private final ModelType type;

	public EcoSpold01ExportWizard(ModelType type) {
		super();
		setNeedsProgressMonitor(true);
		this.type = type;
	}

	@Override
	public void addPages() {
		page = new ModelSelectionPage(type);
		addPage(page);
	}

	@Override
	public void init(final IWorkbench workbench,
			final IStructuredSelection selection) {
		setWindowTitle(Messages.EcoSpoldExportWizard_WindowTitle);
	}

	@Override
	public boolean performFinish() {
		boolean errorOccured = false;
		final List<BaseDescriptor> models = page.getSelectedModels();
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					// set up
					int objectAmount = models.size();
					monitor.beginTask(Messages.EcoSpoldExporting,
							objectAmount + 1);
					monitor.subTask(Messages.EcoSpoldCreatingFolder);
					EcoSpold01Outputter outputter = new EcoSpold01Outputter(
							page.getExportDestination());
					monitor.worked(1);

					try {
						for (BaseDescriptor descriptor : models) {
							if (!monitor.isCanceled()) {
								monitor.subTask(descriptor.getName());
								if (type == ModelType.PROCESS) {
									Process process = new ProcessDao(Database
											.get()).getForId(descriptor.getId());
									outputter.exportProcess(process);
								} else if (type == ModelType.IMPACT_METHOD) {
									ImpactMethod method = new ImpactMethodDao(
											Database.get()).getForId(descriptor
											.getId());
									outputter.exportLCIAMethod(method);
								}
								monitor.worked(1);
							}
						}
					} catch (final Exception e) {
						log.error("Perform finish failed", e);
						throw new InterruptedException(e.getMessage());
					}
					monitor.done();
				}
			});
		} catch (final Exception e) {
			log.error("Perform finish failed", e);
			errorOccured = true;
		}
		return !errorOccured;
	}
}
