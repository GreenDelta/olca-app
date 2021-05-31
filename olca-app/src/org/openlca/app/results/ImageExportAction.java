package org.openlca.app.results;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes a snapshot from a composite and saves this as png - file.
 */
public class ImageExportAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());

	private Composite composite;

	public ImageExportAction(Composite composite) {
		setId("ImageExportAction#composite");
		setToolTipText(M.SaveAsImage);
		setImageDescriptor(Icon.SAVE_AS_IMAGE.descriptor());
		this.composite = composite;
	}

	@Override
	public void run() {
		if (composite == null)
			return;

		log.trace("Take image snapshot");
		Point size = composite.getSize();
		Image image = new Image(composite.getDisplay(), size.x, size.y);
		GC gc = new GC(composite);
		gc.copyArea(image, 0, 0);

		try {
			writeToFile(image);
		} catch (Exception e) {
			log.error("Failed to save export image", e);
		} finally {
			gc.dispose();
			image.dispose();
		}
	}

	private void writeToFile(Image image) {
		var file = FileChooser.forSavingFile(M.Export, "openlca_chart.png");
		if (file == null)
			return;
		log.trace("Export image to {}", file);
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { image.getImageData() };
		loader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);
	}

}
