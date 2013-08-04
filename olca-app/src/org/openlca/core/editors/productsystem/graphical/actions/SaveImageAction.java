/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.actions;

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
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.editors.productsystem.graphical.ProductSystemGraphEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for saving the graph as image
 * 
 * @author Sebastian Greve
 * 
 */
public class SaveImageAction extends Action {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The product system graph editor
	 */
	public ProductSystemGraphEditor editor;

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.SAVE_AS_IMAGE_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.SaveAsImage;
	}

	@Override
	public void run() {
		if (editor != null) {
			final FileDialog dialog = new FileDialog(UI.shell(), SWT.SAVE);
			dialog.setText(Messages.SaveAsImage);
			dialog.setFileName("graph.png");
			dialog.setFilterExtensions(new String[] { "*.png" });
			dialog.setFilterNames(new String[] { "*.png (Portable Network Graphics (PNG)" });
			final String fileName = dialog.open();
			if (fileName != null && fileName.length() > 0) {
				final File file = new File(fileName);
				boolean write = false;
				if (file.exists()) {
					write = MessageDialog.openQuestion(UI.shell(),
							Messages.FileAlreadyExists,
							Messages.OverwriteFileQuestion);
				} else {
					write = true;
				}
				if (write) {
					try {
						PlatformUI.getWorkbench().getProgressService()
								.busyCursorWhile(new JobListenerWithProgress() {

									@Override
									public void run() {
										jobStarted(NLS.bind(
												Messages.Systems_SavingAsImage,
												file), IProgressMonitor.UNKNOWN);
										final ScalableRootEditPart rootEditPart = (ScalableRootEditPart) editor
												.getGraphicalViewer()
												.getRootEditPart();

										final IFigure rootFigure = rootEditPart
												.getLayer(LayerConstants.PRINTABLE_LAYERS);
										final Rectangle rootFigureBounds = rootFigure
												.getBounds();
										final Image img = new Image(null,
												rootFigureBounds.width,
												rootFigureBounds.height);
										final GC imageGC = new GC(img);
										final Graphics graphics = new SWTGraphics(
												imageGC);
										rootFigure.paint(graphics);

										final ImageLoader imgLoader = new ImageLoader();
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
			}
		}
	}

	/**
	 * Setter of the product system graph editor
	 * 
	 * @param editor
	 *            The product system graph editor
	 */
	public void setEditor(final ProductSystemGraphEditor editor) {
		this.editor = editor;
	}

}
