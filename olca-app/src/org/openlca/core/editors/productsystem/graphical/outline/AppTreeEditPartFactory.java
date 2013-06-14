/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.outline;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;

/**
 * Implementation of {@link EditPartFactory}
 * 
 * @author Sebastian Greve
 * 
 */
public class AppTreeEditPartFactory implements EditPartFactory {

	/**
	 * The product system node
	 */
	private final ProductSystemNode model;

	/**
	 * Creates a new instance
	 * 
	 * @param model
	 *            The product system node
	 */
	public AppTreeEditPartFactory(final ProductSystemNode model) {
		this.model = model;
	}

	@Override
	public EditPart createEditPart(final EditPart context, final Object model) {
		EditPart part = null;
		if (model instanceof ProductSystem) {
			part = new ProductSystemTreeEditPart();
		} else if (model instanceof Process) {
			part = new ProcessTreeEditPart(this.model);
		}
		if (part != null) {
			part.setModel(model);
		}
		return part;
	}
}
