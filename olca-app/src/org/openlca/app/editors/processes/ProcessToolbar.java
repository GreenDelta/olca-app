package org.openlca.app.editors.processes;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.LinkingProperties;
import org.openlca.app.db.LinkingPropertiesPage;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Actions;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.ProductSystemWizard;
import org.openlca.app.wizards.calculation.CalculationWizard;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.io.xls.process.output.ExcelExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessToolbar extends EditorActionBarContributor {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void contributeToToolBar(IToolBarManager toolbar) {
		toolbar.add(Actions.create(M.CreateProductSystem,
			Images.descriptor(ModelType.PRODUCT_SYSTEM, Overlay.NEW),
			() -> createSystem(getProcess())));
		toolbar.add(Actions.create(M.ExportToExcel,
			Images.descriptor(FileType.EXCEL),
			() -> exportToExcel(getProcess())));

		// add direct calculation if the database is not connected
		// to a library
		boolean withDirect = Database.get()
			.getLibraries()
			.isEmpty();
		if (withDirect) {
			toolbar.add(Actions.create("Direct calculation",
				Icon.RUN.descriptor(),
				() -> directCalculation(getProcess())));
		}
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
		var name = Labels.name(p);
		name = name == null ? "process" : name;
		name = name.replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx";
		var file = FileChooser.forSavingFile(M.Export, name);
		if (file == null)
			return;
		var list = Collections.singletonList(Descriptor.of(p));
		var export = new ExcelExport(file, Database.get(), list);
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

	static void directCalculation(Process process) {
		if (process == null)
			return;
		FactCalculationDialog.show(process);
	}

	private static class FactCalculationDialog {

		static void show(Process process) {
			String hint = "The direct calculation creates an in-memory "
						  + "product system of all processes in the database. This only "
						  + "gives correct results when there are unambiguous links "
						  + "between these processes (e.g. every product is only produced "
						  + "by a single process or every product input has a default "
						  + "provider set). You can also check the linking properties of "
						  + "the databases under `Database > Check linking properties`.";
			String[] buttons = {"Run calculation", "Check linking", "Cancel"};

			MessageDialog dialog = new MessageDialog(
				UI.shell(),
				"Direct calculation", // title
				null, // image
				hint,
				MessageDialog.INFORMATION, // image type
				0, // default button
				buttons);

			switch (dialog.open()) {
				case 0:
					runCalculation(process);
					break;
				case 1:
					checkLinking(process);
					break;
				default:
					break;
			}
		}

		static void runCalculation(Process p) {
			CalculationWizard.open(p);
		}

		static void checkLinking(Process process) {
			var ref = new AtomicReference<LinkingProperties>();
			App.runWithProgress("Check database links", () -> {
				var props = LinkingProperties.check(Database.get());
				ref.set(props);
			});
			var props = ref.get();
			if (props == null) {
				MsgBox.error("The linking check gave no results");
				return;
			}
			if (props.multiProviderFlows.isEmpty()
				|| props.processesWithoutProviders.isEmpty()) {
				handleUnambiguousLinks(process, props);
			} else {
				handleAmbiguousLinks(props);
			}
		}

		private static void handleAmbiguousLinks(LinkingProperties props) {
			String msg = "There are ambiguous links between processes "
						 + "in the database. This can lead to different results "
						 + "in the calculation depending on the process linking.";
			String[] buttons = {"Show details", "Cancel"};
			var dialog = new MessageDialog(
				UI.shell(),
				"Direct calculation", // title
				null, // image
				msg,
				MessageDialog.WARNING, // image type
				0, // default button
				buttons);
			if (dialog.open() == 0) {
				LinkingPropertiesPage.show(props);
			}
		}

		private static void handleUnambiguousLinks(
			Process process, LinkingProperties props) {
			String msg = "The processes in the database can be"
						 + " linked unambiguously";
			String[] buttons = {"Run calculation", "Show details", "Cancel"};
			var dialog = new MessageDialog(
				UI.shell(),
				"Direct calculation", // title
				null, // image
				msg,
				MessageDialog.INFORMATION, // image type
				0, // default button
				buttons);
			switch (dialog.open()) {
				case 0:
					runCalculation(process);
					break;
				case 1:
					LinkingPropertiesPage.show(props);
					break;
				default:
					break;
			}
		}
	}
}
