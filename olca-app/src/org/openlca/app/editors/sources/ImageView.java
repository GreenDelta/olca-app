package org.openlca.app.editors.sources;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * When a source has an external file which is an image, this view shows that
 * image.
 */
class ImageView {

	private final Composite comp;
	private final Supplier<File> extFile;

	private Label label;
	private File file;

	protected ImageView(Composite comp, Supplier<File> extFile) {
		this.comp = comp;
		this.extFile = extFile;
		update();
	}

	void update() {
		File f = extFile.get();
		if (Objects.equals(f, file))
			return;
		file = f;

		if (label != null) {
			label.dispose();
			label = null;
		}

		// check that we have an image file
		if (file == null || !file.exists())
			return;
		var parts = file.getName().split("\\.");
		var ext = parts[parts.length - 1].toLowerCase();
		var exts = Arrays.asList(
				"gif", "jpg", "jpeg", "png", "bmp");
		if (!exts.contains(ext))
			return;

		// create the image
		var img = new Image(comp.getDisplay(), file.getAbsolutePath());
		label = new Label(comp, SWT.NONE);
		label.setImage(img);
		label.addDisposeListener(e -> {
			if (!img.isDisposed()) {
				img.dispose();
			}
		});
		comp.getParent().layout();
	}

}
