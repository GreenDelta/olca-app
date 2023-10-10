package org.openlca.app.wizards.io;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.db.Database;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.oneclick.OneClickExport;

public class OneClickExportWizard extends Wizard implements IExportWizard {

	private ModelSelectionPage page;
	private FlowMap flowMap;

	public OneClickExportWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = ModelSelectionPage.forDirectory(ModelType.PROCESS)
				.withExtension(parent -> {
					var comp = UI.composite(parent);
					UI.fillHorizontal(comp);
					UI.gridLayout(comp, 3);
					MappingSelector.on(flowMap -> this.flowMap = flowMap)
							.withSelectionPattern("(?i).*one.*click.*lca.*export.*")
							.render(comp);
				});
		addPage(page);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("One Click LCA - Export");
	}

	@Override
	public boolean performFinish() {
		var models = page.getSelectedModels().stream()
				.filter(d -> d instanceof ProcessDescriptor)
				.map(d -> (ProcessDescriptor) d)
				.toList();
		var export = OneClickExport.of(
				Database.get(), models, page.getExportDestination());
		if (flowMap != null) {
			export.withFlowMap(flowMap);
		}
		try {
			getContainer().run(true, true, monitor -> {
				monitor.beginTask("Export processes", IProgressMonitor.UNKNOWN);
				export.run();
				monitor.done();
			});
			return true;
		} catch (Exception e) {
			ErrorReporter.on("export failed", e);
			return false;
		}
	}
}
