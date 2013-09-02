/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.action;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.Messages;
import org.openlca.app.components.JobListenerWithProgress;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveImageAction extends Action {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private ProductSystemNode model;

	SaveImageAction() {
		setId(ActionIds.SAVE_IMAGE_ACTION_ID);
		setText(Messages.SaveAsImage);
		setImageDescriptor(ImageType.SAVE_AS_IMAGE_ICON.getDescriptor());
	}

	@Override
	public void run() {
		if (model == null)
			return;
		FileDialog dialog = new FileDialog(UI.shell(), SWT.SAVE);
		dialog.setText(Messages.SaveAsImage);
		dialog.setFileName("graph.png");
		dialog.setFilterExtensions(new String[] { "*.png" });
		dialog.setFilterNames(new String[] { "*.png (Portable Network Graphics (PNG)" });
		final String fileName = dialog.open();

		if (fileName == null)
			return;
		if (fileName.length() == 0)
			return;

		final File file = new File(fileName);
		boolean write = false;
		if (file.exists()) {
			write = MessageDialog.openQuestion(UI.shell(),
					Messages.FileAlreadyExists, Messages.OverwriteFileQuestion);
		} else
			write = true;
		if (!write)
			return;

		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new JobListenerWithProgress() {

						@Override
						public void run() {
							jobStarted(NLS.bind(Messages.Systems_SavingAsImage,
									file), IProgressMonitor.UNKNOWN);
							ScalableRootEditPart rootEditPart = (ScalableRootEditPart) model
									.getEditor().getGraphicalViewer()
									.getRootEditPart();

							IFigure rootFigure = rootEditPart
									.getLayer(LayerConstants.PRINTABLE_LAYERS);
							Rectangle rootFigureBounds = rootFigure.getBounds();
							Image img = new Image(null, rootFigureBounds.width,
									rootFigureBounds.height);
							GC imageGC = new GC(img);
							Graphics graphics = new SWTGraphics(imageGC);
							rootFigure.paint(graphics);

							ImageLoader imgLoader = new ImageLoader();
							imgLoader.data = new ImageData[] { img
									.getImageData() };
							imgLoader.save(fileName, SWT.IMAGE_PNG);
							done();
						}
					});
		} catch (final Exception e) {
			log.error("Run error", e);
		}
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

}
