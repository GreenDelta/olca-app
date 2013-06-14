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
package org.openlca.core.editors.analyze.sankey;

import java.util.List;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Layout manager for placing the nodes in the graphical viewer
 * 
 * @author Sebastian Greve
 * 
 */
class GraphLayoutManager extends AbstractLayout {

	/**
	 * Horizontal spacing between process nodes
	 */
	public static int horizontalSpacing = 100;

	/**
	 * Vertical spacing between process nodes
	 */
	public static int verticalSpacing = 200;

	private ProductSystemEditPart diagram;
	private Logger log = LoggerFactory.getLogger(getClass());

	public GraphLayoutManager(ProductSystemEditPart diagram) {
		this.diagram = diagram;
	}

	@Override
	public void layout(IFigure container) {
		log.trace("Layout product system figure");
		if (diagram == null)
			return;
		for (Object aPart : diagram.getChildren()) {
			if (aPart instanceof ProcessEditPart) {
				ProcessEditPart part = (ProcessEditPart) aPart;
				ProcessNode node = (ProcessNode) part.getModel();
				part.getFigure().setBounds(node.getXyLayoutConstraints());
			}
		}
	}

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int hint,
			int hint2) {
		container.validate();
		List<?> children = container.getChildren();
		Rectangle result = new Rectangle().setLocation(container
				.getClientArea().getLocation());
		for (int i = 0; i < children.size(); i++) {
			result.union(((IFigure) children.get(i)).getBounds());
		}
		result.resize(container.getInsets().getWidth(), container.getInsets()
				.getHeight());
		return result.getSize();
	}

	public void layoutTree() {
		log.trace("Apply tree-layout");
		if (diagram != null && diagram.getModel() != null) {
			TreeLayout layout = new TreeLayout();
			layout.layout((ProductSystemNode) diagram.getModel());
		}
	}

}
