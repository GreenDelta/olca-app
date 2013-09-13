/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.analysis.sankey;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;

/**
 * Sets new layout
 * 
 * @author Sebastian Greve
 * 
 */
class XYLayoutCommand extends Command {

	/**
	 * The new layout constraints
	 */
	private Rectangle layout;

	/**
	 * The old layout constraints
	 */
	private Rectangle oldLayout;

	/**
	 * The selected {@link ProcessNode}
	 */
	private ProcessNode processNode;

	@Override
	public boolean canExecute() {
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	/**
	 * Applies the location to the grid
	 * 
	 * @param x
	 *            The new x value
	 * @param y
	 *            The new y value
	 * @return The new location on the grid
	 */
	private Point applyGrid(final int x, final int y) {
		int newX = x;
		int newY = y;

		final int hs = GraphLayoutManager.horizontalSpacing;
		final int vs = GraphLayoutManager.verticalSpacing;
		final int gridWidth = ProcessFigure.WIDTH + hs;
		final int gridHeight = ProcessFigure.HEIGHT + vs;

		if (newX <= hs) {
			newX = hs;
		} else {
			final int xMod = (newX - hs) % gridWidth;

			if (xMod != 0) {
				if (xMod <= gridWidth / 2) {
					newX = x - xMod;
				} else {
					newX = x - xMod + gridWidth;
				}
			}
		}

		if (newY <= vs) {
			newY = vs;
		} else {
			final int yMod = (newY - vs) % gridHeight;

			if (yMod != 0) {
				if (yMod <= gridHeight / 2) {
					newY = y - yMod;
				} else {
					newY = y - yMod + gridHeight;
				}
			}
		}

		return new Point(newX, newY);
	}

	@Override
	public void execute() {
		oldLayout = processNode.getXyLayoutConstraints();

		final Point newLocation = applyGrid(layout.x, layout.y);

		processNode.setXyLayoutConstraints(new Rectangle(newLocation.x,
				newLocation.y, oldLayout.width, oldLayout.height));
	}

	@Override
	public String getLabel() {
		return Messages.Move;
	}

	@Override
	public void redo() {
		execute();
	}

	/**
	 * Setter of {@link #layout}
	 * 
	 * @param rect
	 *            - the new layout constraints
	 */
	public void setConstraint(final Rectangle rect) {
		layout = rect;
	}

	/**
	 * Setter of {@link #processNode}
	 * 
	 * @param processNode
	 *            - the selected ProcessNode
	 */
	public void setProcessNode(final ProcessNode processNode) {
		this.processNode = processNode;
	}

	@Override
	public void undo() {
		processNode.setXyLayoutConstraints(oldLayout);
	}
}
