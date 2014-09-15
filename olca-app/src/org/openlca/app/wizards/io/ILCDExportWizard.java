package org.openlca.app.wizards.io;

import java.io.File;
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
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.io.ilcd.ILCDExport;

/**
 * Wizard for exporting processes, flows, flow properties and unit group to the
 * ILCD format
 */
public class ILCDExportWizard extends Wizard implements IExportWizard {

	private ModelSelectionPage exportPage;
	private final ModelType type;

	public ILCDExportWizard(ModelType type) {
		setNeedsProgressMonitor(true);
		this.type = type;
	}

	@Override
	public void addPages() {
		exportPage = new ModelSelectionPage(type);
		addPage(exportPage);
	}

	@Override
	public void init(final IWorkbench workbench,
			final IStructuredSelection selection) {
		setWindowTitle(Messages.ExportILCD);
	}

	@Override
	public boolean performFinish() {
		final IDatabase database = Database.get();
		if (database == null)
			return false;
		final File targetDir = exportPage.getExportDestination();
		if (targetDir == null || !targetDir.isDirectory()) {
			return false;
		}
		final List<BaseDescriptor> components = exportPage.getSelectedModels();

		boolean errorOccured = false;
		try {

			IRunnableWithProgress runnable = new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.Export, components.size());
					int worked = 0;
					ILCDExport export = new ILCDExport(targetDir);
					for (BaseDescriptor descriptor : components) {
						if (monitor.isCanceled())
							break;
						monitor.setTaskName(descriptor.getName());
						try {
							Object component = database.createDao(
									descriptor.getModelType().getModelClass())
									.getForId(descriptor.getId());
							if (component instanceof CategorizedEntity)
								export.export((CategorizedEntity) component,
										database);
						} catch (Exception e) {
							throw new InvocationTargetException(e);
						} finally {
							monitor.worked(++worked);
						}
					}
					export.close();
				}
			};

			// run it in the wizard container
			getContainer().run(true, true, runnable);

		} catch (final Exception e) {
			errorOccured = true;
		}

		return !errorOccured;
	}
}
