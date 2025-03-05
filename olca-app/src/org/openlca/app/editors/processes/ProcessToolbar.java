package org.openlca.app.editors.processes;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.db.LinkingProperties;
import org.openlca.app.db.LinkingPropertiesPage;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Actions;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.ProductSystemWizard;
import org.openlca.app.wizards.calculation.CalculationWizard;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.io.xls.process.XlsProcessWriter;
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
		var withDirect = Libraries.get().isEmpty();
		if (withDirect) {
			toolbar.add(Actions.create(M.DirectCalculation,
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
		name = name.replaceAll("\\W+", "_") + ".xlsx";
		var file = FileChooser.forSavingFile(M.Export, name);
		if (file == null)
			return;
		var export = XlsProcessWriter.of(Database.get());
		App.run(M.ExportProcess,
				() -> export.write(p, file),
				() -> Popup.info(M.ExportDone));
	}

	static void createSystem(Process process) {
		if (process == null)
			return;
		try {
			var w = PlatformUI.getWorkbench()
					.getNewWizardRegistry()
					.findWizard("wizards.new.productsystem")
					.createWizard();
			if (!(w instanceof ProductSystemWizard wizard))
				return;
			wizard.setProcess(process);
			var dialog = new WizardDialog(UI.shell(), wizard);
			if (dialog.open() == Window.OK) {
				Navigator.refresh(Navigator.findElement(ModelType.PRODUCT_SYSTEM));
			}
		} catch (Exception e) {
			ErrorReporter.on("failed to open product system dialog for process", e);
		}
	}

	static void directCalculation(Process process) {
		if (process == null)
			return;
		FactCalculationDialog.show(process);
	}

	private static class FactCalculationDialog {

		static void show(Process process) {
			String hint = M.DirectCalculationUnambiguousLinksInfo;
			String[] buttons = {M.RunCalculation, M.CheckLinking, M.Cancel};

			MessageDialog dialog = new MessageDialog(
					UI.shell(),
					M.DirectCalculation, // title
					null, // image
					hint,
					MessageDialog.INFORMATION, // image type
					0, // default button
					buttons);

			switch (dialog.open()) {
				case 0 -> CalculationWizard.open(process);
				case 1 -> checkLinking(process);
				default -> {
				}
			}
		}

		static void checkLinking(Process process) {
			var ref = new AtomicReference<LinkingProperties>();
			App.runWithProgress(M.CheckDatabaseLinks, () -> {
				var props = LinkingProperties.check(Database.get());
				ref.set(props);
			});
			var props = ref.get();
			if (props == null) {
				MsgBox.error(M.LinkingCheckGaveNoResults);
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
			String msg = M.AmbiguousLinksInfo;
			String[] buttons = {M.ShowDetails, M.Cancel};
			var dialog = new MessageDialog(
					UI.shell(),
					M.DirectCalculation, // title
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
			var msg = M.ProcessesCanBeLinkedUnambiguously;
			String[] buttons = {M.RunCalculation, M.ShowDetails, M.Cancel};
			var dialog = new MessageDialog(
					UI.shell(),
					M.DirectCalculation, // title
					null, // image
					msg,
					MessageDialog.INFORMATION, // image type
					0, // default button
					buttons);
			switch (dialog.open()) {
				case 0 -> CalculationWizard.open(process);
				case 1 -> LinkingPropertiesPage.show(props);
				default -> {
				}
			}
		}
	}
}
