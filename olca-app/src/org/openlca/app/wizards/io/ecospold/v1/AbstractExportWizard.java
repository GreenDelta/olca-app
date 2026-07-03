package org.openlca.app.wizards.io.ecospold.v1;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.io.MappingSelector;
import org.openlca.app.wizards.io.ModelSelectionPage;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.io.ecospold1.output.EcoSpold1Export;

abstract class AbstractExportWizard extends Wizard implements IExportWizard {

	private final ModelType type;
	private final EcoSpold1Export.EcoSpold1Config config;
	private ModelSelectionPage modelPage;

	public AbstractExportWizard(ModelType type) {
		super();
		setNeedsProgressMonitor(true);
		this.type = type;
		this.config = EcoSpold1Export.of(Database.get());
	}

	@Override
	public void addPages() {
		modelPage = ModelSelectionPage.forDirectory(type)
				.withExtension(parent -> {
					var comp = UI.composite(parent);
					UI.stretchX(comp);
					UI.gridLayout(comp, 3);
					MappingSelector.on(config::withFlowMap).render(comp);
				});
		addPage(modelPage);
		if (type == ModelType.PROCESS) {
			addPage(new ExportConfigPage(config));
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ExportEcoSpold);
	}

	@Override
	public boolean performFinish() {
		var models = modelPage.getSelectedModels();
		var res = config
			.withDir(modelPage.getExportDestination())
			.create();
		if (res.isError()) {
			ErrorReporter.on("EcoSpold I export failed", res.error());
			return false;
		}

		try (var export = res.value()) {
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
			List<RootDescriptor> models,
			IProgressMonitor monitor,
			EcoSpold1Export export
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
