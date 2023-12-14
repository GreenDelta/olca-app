package org.openlca.app.wizards.io;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.io.ecospold1.output.EcoSpold1Export;
import org.openlca.io.ecospold1.output.ExportConfig;

import java.util.List;

/**
 * Wizard for exporting processes and impact methods to the EcoSpold01 format
 */
public class EcoSpold01ExportWizard extends Wizard implements IExportWizard {

	private final ModelType type;
	private ModelSelectionPage modelPage;
	private Es1ExportConfigPage configPage;

	public EcoSpold01ExportWizard(ModelType type) {
		super();
		setNeedsProgressMonitor(true);
		this.type = type;
	}

	@Override
	public void addPages() {
		modelPage = ModelSelectionPage.forDirectory(type);
		addPage(modelPage);
		configPage = new Es1ExportConfigPage();
		addPage(configPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ExportEcoSpold);
	}

	@Override
	public boolean performFinish() {
		var models = modelPage.getSelectedModels();
		var config = configPage == null
				? ExportConfig.getDefault()
				: configPage.getConfig();
		try (var export = new EcoSpold1Export(
				modelPage.getExportDestination(), config)) {
			getContainer().run(true, true, monitor -> {
				int size = models.size();
				monitor.beginTask(M.ExportingProcesses, size + 1);
				monitor.subTask(M.CreatingEcoSpoldFolder);
				monitor.worked(1);
				doExport(models, monitor, export);
				monitor.done();
			});
			return true;
		} catch (Exception e) {
			ErrorReporter.on("EcoSpold I export failed", e);
			return false;
		}
	}

	private void doExport(
			List<RootDescriptor> models, IProgressMonitor monitor, EcoSpold1Export export
	) throws InterruptedException {
		try {
			var db = Database.get();
			for (var d : models) {
				if (monitor.isCanceled())
					break;
				monitor.subTask(d.name);
				if (type == ModelType.PROCESS) {
					var process = db.get(Process.class, d.id);
					export.export(process);
				} else if (type == ModelType.IMPACT_METHOD) {
					var method = db.get(ImpactMethod.class, d.id);
					export.export(method);
				}
				monitor.worked(1);
			}
		} catch (Exception e) {
			throw new InterruptedException(e.getMessage());
		}
	}
}
