/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.analyze;

import java.io.File;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.core.editors.analyze.sankey.SankeyDiagram;
import org.openlca.core.editors.io.ui.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for the image export of a Sankey diagram.
 */
class SaveImageAction extends Action {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	public SankeyDiagram sankeyDiagram;

	@Override
	public String getText() {
		return Messages.Common_SaveAsImage;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.SAVE_AS_IMAGE_ICON.getDescriptor();
	}

	public void setSankeyDiagram(final SankeyDiagram sankeyDiagram) {
		this.sankeyDiagram = sankeyDiagram;
	}

	@Override
	public void run() {
		if (sankeyDiagram == null)
			return;
		File file = FileChooser.forExport(".png", "sankey.png");
		if (file == null)
			return;
		Image image = createImage();
		if (image == null)
			return;
		ImageWriter writer = new ImageWriter(file, image);
		App.run(Messages.Sankey_SavingAsImage, writer);
	}

	private Image createImage() {
		try {
			ScalableRootEditPart rootEditPart = (ScalableRootEditPart) sankeyDiagram
					.getGraphicalViewer().getRootEditPart();
			IFigure rootFigure = rootEditPart
					.getLayer(LayerConstants.PRINTABLE_LAYERS);
			Rectangle rootFigureBounds = rootFigure.getBounds();
			Image img = new Image(null, rootFigureBounds.width,
					rootFigureBounds.height);
			GC imageGC = new GC(img);
			Graphics graphics = new SWTGraphics(imageGC);
			rootFigure.paint(graphics);
			return img;
		} catch (Exception e) {
			log.error("Could not create image", e);
			return null;
		}
	}

	private class ImageWriter implements Runnable {

		private File file;
		private Image image;

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
