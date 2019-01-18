package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.preferencepages.IoPreference;
import org.openlca.core.database.Daos;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.io.ilcd.ILCDExport;
import org.openlca.io.ilcd.output.ExportConfig;

/**
 * Wizard for exporting processes, flows, flow properties and unit group to the
 * ILCD format
 */
public class ILCDExportWizard extends Wizard implements IExportWizard {

	private ModelSelectionPage exportPage;

	public ILCDExportWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		ModelType[] types = { ModelType.PRODUCT_SYSTEM,
				ModelType.IMPACT_METHOD, ModelType.PROCESS,
				ModelType.FLOW, ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP, ModelType.ACTOR,
				ModelType.SOURCE };
		exportPage = ModelSelectionPage.forFile("zip", types);
		addPage(exportPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ExportILCD);
	}

	@Override
	public boolean performFinish() {
		File target = exportPage.getExportDestination();
		if (target == null)
			return false;
		List<BaseDescriptor> descriptors = exportPage.getSelectedModels();
		boolean errorOccured = false;
		ExportConfig config = createConfig(target);
		try {
			getContainer().run(true, true,
					monitor -> runExport(monitor, config, descriptors));
		} catch (Exception e) {
			errorOccured = true;
		}
		return !errorOccured;
	}

	private void runExport(IProgressMonitor monitor, ExportConfig config,
			List<BaseDescriptor> descriptors) throws InvocationTargetException {
		monitor.beginTask(M.Export, descriptors.size());
		int worked = 0;
		ILCDExport export = new ILCDExport(config);
		for (BaseDescriptor d : descriptors) {
			if (monitor.isCanceled())
				break;
			monitor.setTaskName(d.name);
			try {
				Object obj = Daos.root(
						config.db, d.type).getForId(d.id);
				if (obj instanceof CategorizedEntity)
					export.export((CategorizedEntity) obj);
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			} finally {
				monitor.worked(++worked);
			}
		}
		export.close();
	}

	private ExportConfig createConfig(File targetDir) {
		ExportConfig config = new ExportConfig(Database.get(), targetDir);
		config.lang = IoPreference.getIlcdLanguage();
		return config;
	}

}
