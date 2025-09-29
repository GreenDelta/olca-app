package org.openlca.app.editors.sd;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
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
	private Canvas imageCanvas;

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

		// Create canvas for image display
		imageCanvas = new Canvas(comp, SWT.BORDER);
		UI.gridData(imageCanvas, true, true).minimumHeight = 300;
		imageCanvas.setBackground(Colors.white());

		// Load and display the image
		displayModelImage();
	}

	private void runSimulation() {
		var sim = Simulator.of(editor.xmile());
		if (sim.hasError()) {
			MsgBox.error("Failed to create simulator", sim.error());
			return;
		}
		SdResultEditor.open(editor.modelName(), sim.value());
	}

	private void displayModelImage() {
		imageCanvas.addPaintListener(e -> {
			var gc = e.gc;
			var canvasWidth = imageCanvas.getBounds().width;
			var canvasHeight = imageCanvas.getBounds().height;

			var imageResult = SystemDynamics.getModelImage(editor.modelDir());
			if (imageResult.hasError()) {
				drawErrorMessage(gc, "No model image available", canvasWidth, canvasHeight);
				return;
			}

			var imageFile = imageResult.value();
			if (!imageFile.exists()) {
				drawErrorMessage(gc, "No model image available", canvasWidth, canvasHeight);
				return;
			}

			try {
				var imageData = new ImageData(imageFile.getAbsolutePath());
				var image = new Image(null, imageData);

				// Calculate scaling to fit canvas while maintaining aspect ratio
				double scaleX = (canvasWidth - 20.0) / imageData.width; // 10px margin on each side
				double scaleY = (canvasHeight - 20.0) / imageData.height; // 10px margin top/bottom
				double scale = Math.min(scaleX, scaleY);
				scale = Math.min(scale, 1.0); // Don't scale up

				// Center the image
				int scaledWidth = (int) (imageData.width * scale);
				int scaledHeight = (int) (imageData.height * scale);
				int x = (canvasWidth - scaledWidth) / 2;
				int y = (canvasHeight - scaledHeight) / 2;

				// Draw scaled image
				var scaledData = imageData.scaledTo(scaledWidth, scaledHeight);
				var scaledImage = new Image(null, scaledData);
				gc.drawImage(scaledImage, x, y);

				image.dispose();
				scaledImage.dispose();
			} catch (Exception ex) {
				ErrorReporter.on("Failed to load model image", ex);
				drawErrorMessage(gc, "Failed to load model image: " + ex.getMessage(), canvasWidth, canvasHeight);
			}
		});
	}

	private void changeImage() {
		var file = FileChooser.open("*.png");
		if (file == null)
			return;

		var modelDir = editor.modelDir();
		var targetFile = new File(modelDir, "model-image.png");

		try {
			Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			imageCanvas.redraw(); // Refresh the image display
			MsgBox.info("Image updated", "The model image has been updated successfully.");
		} catch (Exception e) {
			ErrorReporter.on("Failed to update model image", e);
			MsgBox.error("Failed to update image", "Could not update the model image: " + e.getMessage());
		}
	}

	private void drawErrorMessage(org.eclipse.swt.graphics.GC gc, String message, int width, int height) {
		gc.setForeground(Colors.systemColor(SWT.COLOR_DARK_GRAY));
		var textExtent = gc.textExtent(message);
		int x = (width - textExtent.x) / 2;
		int y = (height - textExtent.y) / 2;
		gc.drawText(message, x, y, true);
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
