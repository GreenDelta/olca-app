package org.openlca.app.wizards;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.SystemDynamics;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.io.WizFileSelector;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.sd.eqn.EvaluationOrder;
import org.openlca.sd.model.SdModel;

public class SdModelWizard extends Wizard implements INewWizard {

	private SdModelWizardPage page;

	public static void open() {
		var wizard = new SdModelWizard();
		var dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("New system dynamics model");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = new SdModelWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		var modelName = checkName(page.nameText.getText());
		if (modelName.isError()) {
			MsgBox.error("Invalid name", modelName.error());
			return false;
		}

		var file = page.selectedFile;
		if (file == null) {
			MsgBox.error("No file selected", "No model file was selected.");
			return false;
		}

		try {
			var ref = new AtomicReference<Res<File>>();
			getContainer().run(true, false, monitor -> {
				monitor.beginTask("Creating the model", IProgressMonitor.UNKNOWN);
				ref.set(createModel(modelName.value(), file));
				monitor.done();
			});

			var res = ref.get();
			if (res == null) {
				MsgBox.error("Model creation did not complete without any information.");
				return false;
			}

			if (res.isError()) {
				MsgBox.error("Invalid model file", res.error());
				return false;
			}

			// Open the SD model editor
			var modelDir = res.value();
			SdModelEditor.open(modelDir);
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			ErrorReporter.on("failed to create SD model", e);
			return false;
		}
	}

	private Res<String> checkName(String name) {
		if (Strings.isBlank(name))
			return Res.error("The name cannot be empty.");
		var sanitized = name.strip().replaceAll("[<>:\"/\\\\|?*]", "_");
		for (var dir : SystemDynamics.getModelDirsOf(Database.get())) {
			if (sanitized.equalsIgnoreCase(dir.getName()))
				return Res.error("A model with this name already exists.");
		}
		return Res.ok(sanitized);
	}

	private Res<File> createModel(String name, File file) {
		try {
			var model = SdModel.readFrom(file);
			if (model.isError())
				return model.wrapError("Failed to read model file: " + file);
			var order = EvaluationOrder.of(model.value().vars());
			if (order.isError()) {
				return order.wrapError(
						"Failed to create evaluation order of vars in file: " + file);
			}
			var dir = SystemDynamics.createModelDir(name, Database.get());
			if (dir.isError())
				return dir;
			var modelFile = new File(dir.value(), "model.xml");
			Files.copy(
					file.toPath(),
					modelFile.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			return Res.ok(dir.value());
		} catch (Exception e) {
			return Res.error("Failed to create model folder", e);
		}
	}

	private static class SdModelWizardPage extends WizardPage {

		private Text nameText;
		private File selectedFile;

		protected SdModelWizardPage() {
			super("SdModelWizardPage");
			setTitle("New system dynamics model");
			setDescription("Create a new system dynamics model");
			setPageComplete(false);
		}

		@Override
		public void createControl(Composite parent) {
			var comp = UI.composite(parent);
			UI.gridLayout(comp, 3);
			setControl(comp);

			nameText = UI.labeledText(comp, M.Name);
			nameText.setText("New model");
			nameText.addModifyListener(e -> checkInput());
			UI.filler(comp);

			var fileSelector = WizFileSelector.on(this::onFileSelected)
					.withLabel("Model file (XMILE)")
					.withDialogTitle("Select a model file")
					.withExtensions("*.xml", "*.stmx");
			fileSelector.render(comp);

			checkInput();
		}

		private void onFileSelected(File file) {
			this.selectedFile = file;
			checkInput();
		}

		private void checkInput() {
			if (nameText.getText().trim().isEmpty()) {
				setPageComplete(false);
				return;
			}
			if (selectedFile == null) {
				setPageComplete(false);
				return;
			}
			setPageComplete(true);
		}
	}
}
