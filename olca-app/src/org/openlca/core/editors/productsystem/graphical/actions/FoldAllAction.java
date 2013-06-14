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
import org.openlca.core.application.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.resources.ImageType;

/**
 * Folds all process nodes in the graphical editor (except the reference
 * process, restores the initial state)
 * 
 * @author Sebastian Greve
 * 
 */
public class FoldAllAction extends Action {

	/**
	 * The actual model
	 */
	private ProductSystemNode model;

	/**
	 * Disposes the action
	 */
	public void dispose() {
		model = null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.FOLD_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.Systems_FoldAllAction_Text;
	}

	@Override
	public void run() {
		model.getEditor().reset(false);
	}

	/**
	 * Setter of the model-field
	 * 
	 * @param model
	 *            The actual model
	 */
	public void setModel(final ProductSystemNode model) {
		this.model = model;
	}

}
