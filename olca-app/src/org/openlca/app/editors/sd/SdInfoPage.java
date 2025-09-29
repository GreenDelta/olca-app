package org.openlca.app.editors.sd;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.SystemDynamics;
import org.openlca.app.util.UI;
import org.openlca.sd.eqn.Simulator;
import org.openlca.sd.xmile.Xmile;

class SdInfoPage extends FormPage {

	private final SdModelEditor editor;
	private SdImageView imageView;

	SdInfoPage(SdModelEditor editor) {
		super(editor, "SdModelInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "System dynamics model: " + editor.modelName());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		infoSection(body, tk);
		imageSection(body, tk);
	}

	private void infoSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		UI.gridLayout(comp, 3);

		var nameText = UI.labeledText(comp, tk, M.Name);
		nameText.setEditable(false);
		nameText.setText(editor.modelName());
		UI.filler(comp, tk);

		var specs = SimSpecs.of(editor.xmile());

		var methodText = UI.labeledText(comp, tk, "Solver method");
		methodText.setEditable(false);
		methodText.setText(specs.method);
		UI.filler(comp, tk);

		var startText = UI.labeledText(comp, tk, "Start time");
		startText.setEditable(false);
		startText.setText(Double.toString(specs.start));
		UI.label(comp, tk, specs.timeUnit);

		var endText = UI.labeledText(comp, tk, "Stop time");
		endText.setEditable(false);
		endText.setText(Double.toString(specs.stop));
		UI.label(comp, tk, specs.timeUnit);

		var dtText = UI.labeledText(comp, tk, "Δt");
		dtText.setEditable(false);
		dtText.setText(Double.toString(specs.dt));
		UI.label(comp, tk, specs.timeUnit);

		UI.filler(comp, tk);
		var btn = UI.button(comp, tk, "Run simulation");
		btn.setImage(Icon.RUN.get());
		Controls.onSelect(btn, e -> runSimulation());
	}

	private void imageSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Model graph");
		UI.gridData(section, true, true);

		// Add action button for changing image
		var onChangeImage = Actions.create("Change image", Icon.EDIT.descriptor(), this::changeImage);
		Actions.bind(section, onChangeImage);

		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		// Create scrollable composite for the image
		var scrolled = new ScrolledComposite(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		UI.gridData(scrolled, true, true).minimumHeight = 300;
		scrolled.setBackground(Colors.white());
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);

		// Create image view
		imageView = new SdImageView(scrolled, this::getModelImageFile);
	}

	private void runSimulation() {
		var sim = Simulator.of(editor.xmile());
		if (sim.hasError()) {
			MsgBox.error("Failed to create simulator", sim.error());
			return;
		}
		SdResultEditor.open(editor.modelName(), sim.value());
	}

	private File getModelImageFile() {
		var imageResult = SystemDynamics.getModelImage(editor.modelDir());
		if (imageResult.hasError()) {
			return null;
		}
		var imageFile = imageResult.value();
		return imageFile.exists() ? imageFile : null;
	}

	private void changeImage() {
		var file = FileChooser.open("*.png");
		if (file == null)
			return;

		var modelDir = editor.modelDir();
		var targetFile = new File(modelDir, "model-image.png");

		try {
			Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			if (imageView != null) {
				imageView.forceUpdate(); // Force refresh the image display
			}
			MsgBox.info("Image updated", "The model image has been updated successfully.");
		} catch (Exception e) {
			ErrorReporter.on("Failed to update model image", e);
			MsgBox.error("Failed to update image", "Could not update the model image: " + e.getMessage());
		}
	}

	/**
	 * A simple image view for displaying model images in a scrollable area,
	 * similar to the ImageView used in SourceInfoPage.
	 */
	private static class SdImageView {

		private final ScrolledComposite scrolled;
		private final java.util.function.Supplier<File> fileSupplier;
		private Label label;
		private File currentFile;
		private long lastModified;

		SdImageView(ScrolledComposite scrolled, java.util.function.Supplier<File> fileSupplier) {
			this.scrolled = scrolled;
			this.fileSupplier = fileSupplier;
			update();
		}

		void update() {
			File file = fileSupplier.get();
			long modified = file != null && file.exists() ? file.lastModified() : 0;

			// Only skip update if file is the same AND modification time hasn't changed
			if (java.util.Objects.equals(file, currentFile) && modified == lastModified)
				return;

			currentFile = file;
			lastModified = modified;
			refreshImage();
		}

		void forceUpdate() {
			currentFile = null; // Reset to force update
			lastModified = 0;
			update();
		}

		private void refreshImage() {
			if (label != null) {
				label.dispose();
				label = null;
			}

			// Create placeholder or image
			if (currentFile == null || !currentFile.exists()) {
				createPlaceholder();
				return;
			}

			// Check if it's an image file
			var fileName = currentFile.getName().toLowerCase();
			if (!(fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
				  fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
				  fileName.endsWith(".bmp"))) {
				createPlaceholder();
				return;
			}

			// Create the image label
			try {
				var image = new Image(scrolled.getDisplay(), currentFile.getAbsolutePath());
				label = new Label(scrolled, SWT.NONE);
				label.setImage(image);
				label.addDisposeListener(e -> {
					if (!image.isDisposed()) {
						image.dispose();
					}
				});

				// Set up scrolled composite
				scrolled.setContent(label);
				var size = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				scrolled.setMinSize(size);
				scrolled.layout();
			} catch (Exception e) {
				ErrorReporter.on("Failed to load image: " + currentFile.getAbsolutePath(), e);
				createPlaceholder();
			}
		}

		private void createPlaceholder() {
			label = new Label(scrolled, SWT.NONE);
			label.setText("No model image available");
			label.setForeground(Colors.systemColor(SWT.COLOR_DARK_GRAY));
			scrolled.setContent(label);
			var size = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			scrolled.setMinSize(size);
			scrolled.layout();
		}
	}

	private record SimSpecs(
			double start,
			double stop,
			double dt,
			String timeUnit,
			String method
	) {

		static SimSpecs of(Xmile xmile) {
			if (xmile == null || xmile.simSpecs() == null)
				return new SimSpecs(0, 0, 0, "", "");
			var specs = xmile.simSpecs();
			double dt = 1;
			if (specs.dt() != null && specs.dt().value() != null) {
				dt = specs.dt().reciprocal() != null && specs.dt().reciprocal()
						? 1 / specs.dt().value()
						: specs.dt().value();
			}

			return new SimSpecs(
					specs.start() != null ? specs.start() : 0,
					specs.stop() != null ? specs.stop() : 0,
					dt,
					specs.timeUnits() != null ? specs.timeUnits() : "",
					specs.method() != null ? specs.method() : ""
			);
		}
	}

}
