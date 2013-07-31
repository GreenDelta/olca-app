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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutManager;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutType;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;

/**
 * Creates a {@link LayoutCommand} and executes it. Also sets checkedAction in
 * graphical editor to this
 * 
 * @see Action
 * 
 * @author Sebastian Greve
 * 
 */
public class LayoutAction extends Action {

	/**
	 * ID of this Action
	 */
	public static final String ID = "SwitchLM";

	/**
	 * The product system node
	 */
	private ProductSystemNode node;

	/**
	 * The type of layout this action should perform
	 */
	private GraphLayoutType type;

	/**
	 * Constructor of this Action
	 * 
	 * @param type
	 *            The type of layout this action should perform
	 */
	public LayoutAction(final GraphLayoutType type) {
		super(type.getDisplayName());
		this.type = type;
	}

	/**
	 * Disposes the action
	 */
	public void dispose() {
		node = null;
		type = null;
	}

	@Override
	public String getId() {
		return ID + type;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		ImageDescriptor descriptor = null;
		switch (type) {
		case TreeLayout:
			descriptor = ImageType.TREE_LAYOUT_ICON.getDescriptor();
			break;
		case MinimalTreeLayout:
			descriptor = ImageType.MINIMAL_TREE_LAYOUT_ICON.getDescriptor();
			break;
		}
		return descriptor;
	}

	@Override
	public String getText() {
		return NLS.bind(Messages.Systems_LayoutAction_Text,
				type.getDisplayName());
	}

	@Override
	public void run() {
		node.getEditor()
				.getCommandStack()
				.execute(
						new LayoutCommand(node.getFigure(),
								(GraphLayoutManager) node.getFigure()
										.getLayoutManager(), type));
	}

	/**
	 * Setter of the productSystemNode-field
	 * 
	 * @param node
	 *            The product system node of the editor
	 */
	public void setNode(final ProductSystemNode node) {
		this.node = node;
	}

}
