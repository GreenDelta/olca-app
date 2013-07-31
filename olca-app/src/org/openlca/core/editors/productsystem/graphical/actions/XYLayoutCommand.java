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

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ProcessFigure;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;

/**
 * Sets new layout
 * 
 * @author Sebastian Greve
 * 
 */
public class XYLayoutCommand extends Command {

	/**
	 * The new layout constraints
	 */
	private Rectangle layout;

	/**
	 * Is this a move command?
	 */
	private boolean move = false;

	/**
	 * The old layout constraints
	 */
	private Rectangle oldLayout;

	/**
	 * The selected {@link ProcessNode}
	 */
	private ProcessNode processNode;

	/**
	 * Is this a resize height command?
	 */
	private boolean resizeHeight = false;

	@Override
	public boolean canExecute() {
		if (processNode.isMinimized() && resizeHeight) {
			return false;
		}
		if (layout.getSize().width < ProcessFigure.minimumWidth
				|| layout.getSize().height < processNode.getFigure()
						.getMinimumHeight() && !processNode.isMinimized()
				|| layout.getSize().height < ProcessFigure.minimumHeight
				&& processNode.isMinimized()) {
			return false;
		}
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		oldLayout = processNode.getXyLayoutConstraints();
		processNode.setXyLayoutConstraints(new Rectangle(!move ? oldLayout.x
				: layout.x, !move ? oldLayout.y : layout.y,
				!move ? layout.width : oldLayout.width,
				!resizeHeight ? oldLayout.height : layout.height));
	}

	@Override
	public String getLabel() {
		String label = null;
		if (move) {
			label = Messages.Systems_XYLayoutCommand_MoveText;
		} else {
			label = Messages.Systems_XYLayoutCommand_ResizeText;
		}
		return label;
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
	 * Setter of the move-field
	 * 
	 * @param move
	 *            is this a move command?
	 */
	public void setMove(final boolean move) {
		this.move = move;
	}

	/**
	 * Setter of the processNode-field
	 * 
	 * @param processNode
	 *            the selected process node
	 */
	public void setProcessNode(final ProcessNode processNode) {
		this.processNode = processNode;
	}

	/**
	 * Setter of the resizeHeight-field
	 * 
	 * @param resizeHeight
	 *            is this a resize height command?
	 */
	public void setResizeHeight(final boolean resizeHeight) {
		this.resizeHeight = resizeHeight;
	}

	@Override
	public void undo() {
		processNode.setXyLayoutConstraints(oldLayout);
	}
}
