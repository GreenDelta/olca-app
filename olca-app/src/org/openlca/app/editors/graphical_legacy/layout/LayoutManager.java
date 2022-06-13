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
package org.openlca.app.editors.graphical_legacy.layout;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;

public class LayoutManager extends AbstractLayout {

	public static int H_SPACE = 25;
	public static int V_SPACE = 25;

	private ProductSystemNode node;

	public LayoutManager(ProductSystemNode node) {
		this.node = node;
	}

	private void layoutAsMinimalTree() {
		List<ProcessNode> nodes = new ArrayList<>();
		for (ProcessNode child : node.getChildren()) {
			if (child.isVisible())
				nodes.add(child);
			else
				child.setBox(new Rectangle(0, 0, child.getSize().width, child.getSize().height));
		}
		MinimalTreeLayout layout = new MinimalTreeLayout();
		layout.layout(nodes.toArray(new ProcessNode[nodes.size()]));
	}

	private void layoutAsTree() {
		TreeLayout layout = new TreeLayout();
		layout.layout(node);
	}

	private void layoutXY() {
		for (ProcessNode child : node.getChildren()) {
			if (child.figure == null)
				continue;
			child.figure.setBounds(child.getBox());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Dimension calculatePreferredSize(IFigure container, int hint,
			int hint2) {
		container.validate();
		List<IFigure> children = container.getChildren();
		Rectangle result = new Rectangle().setLocation(container.getClientArea().getLocation());
		for (IFigure child : children)
			result.union(child.getBounds());
		result.resize(container.getInsets().getWidth(), container.getInsets().getHeight());
		return result.getSize();
	}

	@Override
	public void layout(IFigure container) {
		Animation.recordInitialState(container);
		if (Animation.playbackState(container))
			return;
		layoutXY();
	}

	public void layout(IFigure container, LayoutType type) {
		Animation.recordInitialState(container);
		if (Animation.playbackState(container))
			return;
		if (type != null)
			switch (type) {
			case TREE_LAYOUT:
				layoutAsTree();
				break;
			case MINIMAL_TREE_LAYOUT:
				layoutAsMinimalTree();
				break;
			}
	}

}
