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

import java.util.ArrayList;

import org.eclipse.gef.commands.Command;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutManager;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutType;
import org.openlca.core.editors.productsystem.graphical.model.ProcessFigure;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;

/**
 * Expands or folds a specific process node
 * 
 * @author Sebastian Greve
 * 
 */
public class ExpandFoldCommand extends Command {

	/**
	 * The figure to be expanded or folded
	 */
	private final ProcessFigure figure;

	/**
	 * Indicates if the command should fold or expand the node
	 */
	private final boolean fold;

	/**
	 * Indicates if it should be expanded/folded to the left or to the right
	 */
	private final boolean left;

	/**
	 * Creates a new expand/fold command
	 * 
	 * @param figure
	 *            The figure to be expanded or folded
	 * @param left
	 *            Indicates if it should be expanded/folded to the left or to
	 *            the right
	 * @param fold
	 *            Indicates if the command should fold or expand the node
	 */
	public ExpandFoldCommand(final ProcessFigure figure, final boolean left,
			final boolean fold) {
		this.figure = figure;
		this.left = left;
		this.fold = fold;
	}

	@Override
	public boolean canExecute() {
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		if (fold) {
			figure.fold(new ArrayList<ProcessNode>(), left, figure
					.getProcessNode().getProcess().getId());
		} else {
			figure.expand(new ArrayList<ProcessNode>(), left, figure
					.getProcessNode().getProcess().getId());
		}
		if (left) {
			figure.setExpandedLeft(!fold);
		} else {
			figure.setExpandedRight(!fold);
		}
		((GraphLayoutManager) ((ProductSystemNode) figure.getProcessNode()
				.getParent()).getFigure().getLayoutManager()).layout(figure,
				((ProductSystemNode) figure.getProcessNode().getParent())
						.getEditor().getLayoutType());
	}

	@Override
	public String getLabel() {
		return fold ? Messages.Systems_ExpandFoldCommand_FoldText
				: Messages.Systems_ExpandFoldCommand_ExpandText;
	}

	@Override
	public void redo() {
		execute();
	}

	@Override
	public void undo() {
		if (fold) {
			figure.expand(new ArrayList<ProcessNode>(), left, figure
					.getProcessNode().getProcess().getId());
		} else {
			figure.fold(new ArrayList<ProcessNode>(), left, figure
					.getProcessNode().getProcess().getId());
		}
		if (left) {
			figure.setExpandedLeft(fold);
		} else {
			figure.setExpandedRight(fold);
		}
		((GraphLayoutManager) ((ProductSystemNode) figure.getProcessNode()
				.getParent()).getFigure().getLayoutManager()).layout(figure,
				GraphLayoutType.TreeLayout);
	}

}
