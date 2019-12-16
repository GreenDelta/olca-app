package org.openlca.app.editors.processes;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.systems.CalculationWizard;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.preferences.FeatureFlag;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Actions;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.ProductSystemWizard;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.xls.process.output.ExcelExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessToolbar extends EditorActionBarContributor {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void contributeToToolBar(IToolBarManager toolbar) {
		toolbar.add(Actions.create(M.CreateProductSystem,
				Images.descriptor(ModelType.PRODUCT_SYSTEM, Overlay.NEW),
				() -> createSystem(getProcess())));
		toolbar.add(Actions.create(M.ExportToExcel,
				Images.descriptor(FileType.EXCEL),
				() -> exportToExcel(getProcess())));

		// fast network calculation
		if (!FeatureFlag.FAST_NETWORK_CALCULATION.isEnabled())
			return;
		toolbar.add(Actions.create("Fast network calculation",
				Icon.RUN.descriptor(),
				() -> {
					Process p = getProcess();
					if (p == null)
						return;
					ProductSystem sys = ProductSystem.from(p);
					sys.withoutNetwork = true;
					CalculationWizard.open(sys);
				}));

	}

	private Process getProcess() {
		ProcessEditor editor = Editors.getActive();
		if (editor == null) {
			log.error("unexpected error: process editor is not active");
			return null;
		}
		return editor.getModel();
	}

	static void exportToExcel(Process p) {
		if (p == null)
			return;
		String name = Labels.getDisplayName(p);
		name = name == null ? "process" : name;
		name = name.replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx";
		File f = FileChooser.forExport("*.xlsx", name);
		if (f == null)
			return;
		List<ProcessDescriptor> list = Arrays.asList(
				Descriptors.toDescriptor(p));
		ExcelExport export = new ExcelExport(f, Database.get(), list);
		App.run(M.ExportProcess, export, () -> {
			Popup.info(M.ExportDone);
		});
	}

	static void createSystem(Process process) {
		if (process == null)
			return;
		try {
			String wizardId = "wizards.new.productsystem";
			IWorkbenchWizard wizard = PlatformUI.getWorkbench()
					.getNewWizardRegistry().findWizard(wizardId).createWizard();
			if (!(wizard instanceof ProductSystemWizard))
				return;
			ProductSystemWizard systemWizard = (ProductSystemWizard) wizard;
			systemWizard.setProcess(process);
			WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
			if (dialog.open() == Window.OK) {
				Navigator.refresh(Navigator.findElement(ModelType.PRODUCT_SYSTEM));
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(ProcessToolbar.class);
			log.error("failed to open product system dialog for process", e);
		}
	}
}
