/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.openlca.core.editors.productsystem.graphical;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.core.editors.productsystem.graphical.model.ProcessFigure;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProcessPart;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemPart;

/**
 * The layout manager for the graphical editor
 * 
 * @author Sebastian Greve
 * 
 */
public class GraphLayoutManager extends AbstractLayout {

	/**
	 * Spacing to the top
	 */
	public static int horizontalSpacing = 25;

	/**
	 * Spacing to the left
	 */
	public static int verticalSpacing = 25;

	/**
	 * The diagram
	 */
	private ProductSystemPart diagram;

	/**
	 * Creates a new layout manager
	 * 
	 * @param diagram
	 *            The diagram
	 */
	public GraphLayoutManager(final ProductSystemPart diagram) {
		this.diagram = diagram;
	}

	/**
	 * Lays out the diagram as a minimal tree
	 */
	private void layoutMinimalTree() {
		final List<ProcessFigure> figures = new ArrayList<>();
		// for each process part
		for (final Object o : diagram.getChildren()) {
			if (o instanceof ProcessPart) {
				final ProcessFigure figure = ((ProcessNode) ((ProcessPart) o)
						.getModel()).getFigure();
				if (figure.isVisible()) {
					// add figure to "figures to layout"
					figures.add(figure);
				} else {
					// set location to (0,0)
					figure.getProcessNode().setXyLayoutConstraints(
							new Rectangle(0, 0, figure.getSize().width, figure
									.getSize().height));
				}
			}
		}
		final MinimalTreeLayout layout = new MinimalTreeLayout();
		// layout the figures
		layout.layout(figures.toArray(new ProcessFigure[figures.size()]));
	}

	/**
	 * Lays out the diagram as a tree
	 */
	private void layoutTree() {
		final TreeLayout layout = new TreeLayout();
		layout.layout((ProductSystemNode) diagram.getModel());
	}

	/**
	 * Lays out the diagram with the xy coordinates of the process nodes
	 */
	private void layoutXY() {
		for (int i = 0; i < diagram.getChildren().size(); i++) {
			if (diagram.getChildren().get(i) instanceof ProcessPart) {
				final ProcessPart part = (ProcessPart) diagram.getChildren()
						.get(i);
				part.getFigure().setBounds(
						((ProcessNode) part.getModel())
								.getXyLayoutConstraints());
			}
		}
	}

	@Override
	protected Dimension calculatePreferredSize(final IFigure container,
			final int hint, final int hint2) {
		container.validate();
		final List<?> children = container.getChildren();
		final Rectangle result = new Rectangle().setLocation(container
				.getClientArea().getLocation());
		for (int i = 0; i < children.size(); i++) {
			result.union(((IFigure) children.get(i)).getBounds());
		}
		result.resize(container.getInsets().getWidth(), container.getInsets()
				.getHeight());
		return result.getSize();
	}

	/**
	 * Disposes the layout manager
	 */
	public void dispose() {
		diagram = null;
	}

	@Override
	public void layout(final IFigure container) {
		GraphAnimation.recordInitialState(container);
		if (GraphAnimation.playbackState(container)) {
			return;
		}
		layoutXY();
	}

	/**
	 * Lays out the given figure
	 * 
	 * @param container
	 *            The figure to be layed out
	 * @param type
	 *            The type of layout to be appliedF
	 */
	public void layout(final IFigure container, final GraphLayoutType type) {
		GraphAnimation.recordInitialState(container);
		if (GraphAnimation.playbackState(container)) {
			return;
		}
		if (type != null) {
			switch (type) {
			case TreeLayout:
				layoutTree();
				break;
			case MinimalTreeLayout:
				layoutMinimalTree();
				break;
			}
		}
	}

}
