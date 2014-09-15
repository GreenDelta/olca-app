package org.openlca.app.editors.graphical.action;

import java.io.File;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.rcp.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SaveImageAction extends Action {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private ProductSystemGraphEditor editor;

	SaveImageAction() {
		setId(ActionIds.SAVE_IMAGE);
		setText(Messages.SaveAsImage);
		setImageDescriptor(ImageType.SAVE_AS_IMAGE_ICON.getDescriptor());
	}

	void setEditor(ProductSystemGraphEditor editor) {
		this.editor = editor;
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		File file = FileChooser.forExport("*.png", "graph.png");
		if (file == null)
			return;
		App.run(Messages.SaveAsImage, new Runner(file));
	}

	private class Runner implements Runnable {

		private File file;

		public Runner(File file) {
			this.file = file;
		}

		@Override
		public void run() {
			if (file == null)
				return;
			log.trace("export product graph as image: {}", file);
			ScalableRootEditPart editPart = (ScalableRootEditPart) editor
					.getGraphicalViewer().getRootEditPart();
			IFigure rootFigure = editPart
					.getLayer(LayerConstants.PRINTABLE_LAYERS);
			Rectangle bounds = rootFigure.getBounds();
			Image img = new Image(null, bounds.width, bounds.height);
			GC imageGC = new GC(img);
			Graphics graphics = new SWTGraphics(imageGC);
			rootFigure.paint(graphics);
			ImageLoader imgLoader = new ImageLoader();
			imgLoader.data = new ImageData[] { img.getImageData() };
			imgLoader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);
		}
	}
}
