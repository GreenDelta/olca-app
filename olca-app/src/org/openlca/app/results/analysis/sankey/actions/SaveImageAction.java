package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.LayerConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Action for the image export of a Sankey diagram.
 */
public class SaveImageAction extends Action {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	public SankeyEditor sankeyEditor;

	public SaveImageAction(SankeyEditor sankeyEditor) {
		this.sankeyEditor = sankeyEditor;
		setId(ActionIds.SAVE_IMAGE);
	}

	@Override
	public String getText() {
		return M.SaveAsImage;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.SAVE_AS_IMAGE.descriptor();
	}

	@Override
	public void run() {
		if (sankeyEditor == null)
			return;
		File file = FileChooser.forSavingFile(M.Export, "sankey.png");
		if (file == null)
			return;
		Image image = createImage();
		if (image == null)
			return;
		ImageWriter writer = new ImageWriter(file, image);
		App.run(M.SavingDiagramAsImageIn, writer);
	}

	private Image createImage() {
		try {
			var root = sankeyEditor.getRootEditPart();
			IFigure figure = root.getLayer(LayerConstants.PRINTABLE_LAYERS);
			Rectangle bounds = figure.getBounds();
			Image img = new Image(null, bounds.width, bounds.height);
			GC imageGC = new GC(img);
			Graphics graphics = new SWTGraphics(imageGC);
			figure.paint(graphics);
			return img;
		} catch (Exception e) {
			log.error("Could not create image", e);
			return null;
		}
	}

	private class ImageWriter implements Runnable {

		private final File file;
		private final Image image;

		public ImageWriter(File file, Image image) {
			this.file = file;
			this.image = image;
		}

		@Override
		public void run() {
			try {
				ImageLoader imgLoader = new ImageLoader();
				imgLoader.data = new ImageData[] { image.getImageData() };
				imgLoader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);
			} catch (Exception e) {
				log.error("Failed to write image", e);
			}
		}
	}
}
