package org.openlca.app.editors.sd.editor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.SystemDynamics;
import org.openlca.app.util.UI;

class ImageSection {

	private final SdModelEditor editor;

	private Composite comp;
	private Label label;

	ImageSection(SdModelEditor editor) {
		this.editor = editor;
	}

	void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Model image");
		UI.gridData(section, true, true);
		var onChange = Actions.create(
				"Change image", Icon.EDIT.descriptor(), this::changeImage);
		Actions.bind(section, onChange);

		comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		loadImage();
	}

	private void loadImage() {
		var res = SystemDynamics.getModelImage(editor.modelDir());
		if (res.isError())
			return;
		if (label != null) {
			label.dispose();
			label = null;
		}

		var path = res.value().getAbsolutePath();
		try {
			var image = new Image(comp.getDisplay(), path);
			label = new Label(comp, SWT.NONE);
			label.setImage(image);
			label.addDisposeListener(e -> {
				if (!image.isDisposed()) {
					image.dispose();
				}
			});
			comp.layout();
		} catch (Exception e) {
			ErrorReporter.on("Failed to load image: " + path, e);
		}
	}

	private void changeImage() {
		var file = FileChooser.open("*.png");
		if (file == null)
			return;
		var modelDir = editor.modelDir();
		var targetFile = new File(modelDir, "model-image.png");
		try {
			Files.copy(
					file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			loadImage();
		} catch (Exception e) {
			ErrorReporter.on("Failed to update model image", e);
		}
	}
}
