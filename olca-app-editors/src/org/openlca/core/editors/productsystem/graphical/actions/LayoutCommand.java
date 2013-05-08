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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutManager;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutType;
import org.openlca.core.editors.productsystem.graphical.model.ProcessFigure;

/**
 * Sets the given {@link GraphLayoutType}
 * 
 * @author Sebastian Greve
 * 
 */
public class LayoutCommand extends Command {

	/**
	 * The figure
	 */
	private final IFigure figure;

	/**
	 * The {@link GraphLayoutManager} of this editor
	 */
	private final GraphLayoutManager layoutManager;

	/**
	 * Saves the old constraints of the process figures
	 */
	private final Map<IFigure, Rectangle> oldConstraints = new HashMap<>();

	/**
	 * The new {@link GraphLayoutType}
	 */
	private final GraphLayoutType type;

	/**
	 * Constructor of a new SwitchLayoutCommand
	 * 
	 * @param figure
	 *            The figure
	 * @param layoutManager
	 *            The {@link GraphLayoutManager} of this editor
	 * @param type
	 *            The new {@link GraphLayoutType}
	 */
	public LayoutCommand(final IFigure figure,
			final GraphLayoutManager layoutManager, final GraphLayoutType type) {
		this.figure = figure;
		this.layoutManager = layoutManager;
		this.type = type;
	}

	@Override
	public boolean canExecute() {
		return type != null && layoutManager != null && figure != null;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		for (final Object o : figure.getChildren()) {
			if (o instanceof ProcessFigure) {
				if (((ProcessFigure) o).isVisible()) {
					oldConstraints.put((IFigure) o, ((IFigure) o).getBounds()
							.getCopy());
				}
			}
		}
		layoutManager.layout(figure, type);
	}

	@Override
	public String getLabel() {
		return Messages.Systems_LayoutCommand_LayoutText
				+ type.getDisplayName();
	}

	@Override
	public void redo() {
		layoutManager.layout(figure, type);
	}

	@Override
	public void undo() {
		for (final Object o : figure.getChildren()) {
			if (o instanceof ProcessFigure) {
				if (oldConstraints.get(o) != null) {
					((ProcessFigure) o).getProcessNode()
							.setXyLayoutConstraints(oldConstraints.get(o));
				}
			}
		}
	}

}
