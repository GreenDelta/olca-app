/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.openlca.app.tools.graphics.frame;

import java.beans.PropertyChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.KeyEvent;
import org.eclipse.draw2d.KeyListener;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 *  <i>This class is a copy of {@link org.eclipse.draw2d.parts.ScrollableThumbnail}
 *  to have the ability to override <code>reconfigureSelectorBounds</code>.</i>
 * <p>
 * A scaled <b>centered</b> image representation of a Figure. If the source Figure is not
 * completely visible, a SelectorFigure is placed over the thumbnail
 * representing the viewable area and can be dragged around to scroll the source
 * figure.
 */
public class MinimapThumbnail extends Thumbnail {

	private class ClickScrollerAndDragTransferrer extends
			MouseMotionListener.Stub implements MouseListener {
		private boolean dragTransfer;

		public void mouseDoubleClicked(MouseEvent me) {
		}

		public void mouseDragged(MouseEvent me) {
			if (dragTransfer)
				syncher.mouseDragged(me);
		}

		public void mousePressed(MouseEvent me) {
			if (!(getClientArea().contains(me
					.getLocation())))
				return;
			Dimension selectorCenter = selector.getBounds().getSize()
					.scale(0.5f);
			Point scrollPoint = me
					.getLocation()
					.getTranslated(getLocation().getNegated())
					.translate(selectorCenter.negate())
					.scale(1.0f / getViewportScaleX(),
							1.0f / getViewportScaleY())
					.translate(viewport.getHorizontalRangeModel().getMinimum(),
							viewport.getVerticalRangeModel().getMinimum());
			viewport.setViewLocation(scrollPoint);
			syncher.mousePressed(me);
			dragTransfer = true;
		}

		public void mouseReleased(MouseEvent me) {
			syncher.mouseReleased(me);
			dragTransfer = false;
		}
	}

	private class ScrollSynchronizer extends MouseMotionListener.Stub implements
			MouseListener {
		private Point startLocation;
		private Point viewLocation;

		public void mouseDoubleClicked(MouseEvent me) {
		}

		public void mouseDragged(MouseEvent me) {
			if (startLocation != null) {
				Dimension d = me.getLocation().getDifference(startLocation);
				d.scale(1.0f / getViewportScaleX(), 1.0f / getViewportScaleY());
				viewport.setViewLocation(viewLocation.getTranslated(d));
				me.consume();
			}
		}

		public void mousePressed(MouseEvent me) {
			startLocation = me.getLocation();
			viewLocation = viewport.getViewLocation();
			me.consume();
		}

		public void mouseReleased(MouseEvent me) {
		}
	}

	private class SelectorFigure extends Figure {

		private final Rectangle iBounds;

		private final ImageData iData;

		public SelectorFigure() {
			PaletteData pData = new PaletteData(0xFF, 0xFF00, 0xFF0000);
			RGB rgb = ColorConstants.menuBackgroundSelected.getRGB();
			int fillColor = pData.getPixel(rgb);
			iData = new ImageData(1, 1, 24, pData);
			iData.setPixel(0, 0, fillColor);
			iData.setAlpha(0, 0, 55);
			iBounds = new Rectangle(0, 0, 1, 1);
		}

		public void paintFigure(Graphics g) {
			Rectangle bounds = getBounds().getCopy();

			// Avoid drawing images that are 0 in dimension
			if (bounds.width < 5 || bounds.height < 5)
				return;

			// Don't paint the selector figure if the entire source is visible.
			Dimension thumbnailSize = new Dimension(getThumbnailImage());
			// expand to compensate for rounding errors in calculating bounds
			Dimension size = getSize().getExpanded(1, 1);
			if (size.contains(thumbnailSize))
				return;

			bounds.height--;
			bounds.width--;

			Image image = new Image(Display.getCurrent(), iData);
			g.drawImage(image, iBounds, bounds);
			image.dispose();

			g.setForegroundColor(ColorConstants.menuBackgroundSelected);
			g.drawRectangle(bounds);
		}
	}

	private final FigureListener figureListener = source -> reconfigureSelectorBounds();
	private final KeyListener keyListener = new KeyListener.Stub() {
		public void keyPressed(KeyEvent ke) {
			int moveX = viewport.getClientArea().width / 4;
			int moveY = viewport.getClientArea().height / 4;
			if (ke.keycode == SWT.HOME
					|| (isMirrored() ? ke.keycode == SWT.ARROW_RIGHT
					: ke.keycode == SWT.ARROW_LEFT))
				viewport.setViewLocation(viewport.getViewLocation().translate(
						-moveX, 0));
			else if (ke.keycode == SWT.END
					|| (isMirrored() ? ke.keycode == SWT.ARROW_LEFT
					: ke.keycode == SWT.ARROW_RIGHT))
				viewport.setViewLocation(viewport.getViewLocation().translate(
						moveX, 0));
			else if (ke.keycode == SWT.ARROW_UP || ke.keycode == SWT.PAGE_UP)
				viewport.setViewLocation(viewport.getViewLocation().translate(
						0, -moveY));
			else if (ke.keycode == SWT.ARROW_DOWN
					|| ke.keycode == SWT.PAGE_DOWN)
				viewport.setViewLocation(viewport.getViewLocation().translate(
						0, moveY));
		}
	};

	private final PropertyChangeListener propListener = evt -> reconfigureSelectorBounds();

	private ScrollSynchronizer syncher;
	private IFigure selector;
	private Viewport viewport;

	/**
	 * Creates a new MinimapThumbnail.
	 */
	public MinimapThumbnail() {
		super();
		initialize();
	}

	/**
	 * Creates a new MinimapThumbnail that synchs with the given Viewport.
	 *
	 * @param port
	 *            The Viewport
	 */
	public MinimapThumbnail(Viewport port) {
		super();
		setViewport(port);
		initialize();
	}

	@Override
	protected void paintFigure(Graphics graphics) {
		var thumbnail = getThumbnailImage();
		if (thumbnail == null)
			return;
		graphics.drawImage(thumbnail, getImageLocation());
	}

	private Point getImageLocation() {
		if (thumbnailImage == null)
			return getClientArea().getLocation();

		var location = getClientArea().getLocation();
		var marginWidth = (getClientArea().width - thumbnailImage.getBounds().width) / 2;
		var marginHeight = (getClientArea().height - thumbnailImage.getBounds().height) / 2;
		var translation = new Point(marginWidth, marginHeight);
		return location.getTranslated(translation);
	}

	@Override
	public void deactivate() {
		unhookViewport();
		unhookSelector();
		super.deactivate();
	}

	private double getViewportScaleX() {
		return (double) targetSize.width
				/ viewport.getContents().getBounds().width;
	}

	private double getViewportScaleY() {
		return (double) targetSize.height
				/ viewport.getContents().getBounds().height;
	}

	private void hookSelector() {
		selector.addMouseListener(syncher = new ScrollSynchronizer());
		selector.addMouseMotionListener(syncher);
		selector.addKeyListener(keyListener);
		add(selector);
	}

	private void hookViewport() {
		viewport.addPropertyChangeListener(Viewport.PROPERTY_VIEW_LOCATION,
				propListener);
		viewport.addFigureListener(figureListener);
	}

	private void initialize() {
		selector = new SelectorFigure();
		selector.setFocusTraversable(true);
		hookSelector();
		var transferrer = new ClickScrollerAndDragTransferrer();
		addMouseListener(transferrer);
		addMouseMotionListener(transferrer);
	}

	private void reconfigureSelectorBounds() {
		Rectangle rect = new Rectangle();
		Point offset = viewport.getViewLocation();
		offset.x -= viewport.getHorizontalRangeModel().getMinimum();
		offset.y -= viewport.getVerticalRangeModel().getMinimum();
		rect.setLocation(offset);
		rect.setSize(viewport.getClientArea().getSize());
		rect.scale(getViewportScaleX(), getViewportScaleY());
		//
		rect.translate(getImageLocation());
		selector.setBounds(rect);
	}

	@Override
	protected void setScales(float scaleX, float scaleY) {
		if (scaleX == getScaleX() && scaleY == getScaleY())
			return;

		super.setScales(scaleX, scaleY);
		reconfigureSelectorBounds();
	}

	/**
	 * Sets the Viewport that this MinimapThumbnail will synch with.
	 *
	 * @param port
	 *            The Viewport
	 */
	public void setViewport(Viewport port) {
		viewport = port;
		hookViewport();
	}

	private void unhookSelector() {
		selector.removeKeyListener(keyListener);
		selector.removeMouseMotionListener(syncher);
		selector.removeMouseListener(syncher);
		remove(selector);
	}

	private void unhookViewport() {
		viewport.removePropertyChangeListener(Viewport.PROPERTY_VIEW_LOCATION,
				propListener);
		viewport.removeFigureListener(figureListener);
	}

}

