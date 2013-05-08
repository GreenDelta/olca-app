/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.model;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

/**
 * The EditPartFactory of this graphical editor
 * 
 * @see EditPartFactory
 * 
 * @author Sebastian Greve
 * 
 */
public class AppEditPartFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(final EditPart context, final Object model) {
		AbstractGraphicalEditPart part = null;
		if (model instanceof ProductSystemNode) {
			// product system part
			part = new ProductSystemPart();
		} else if (model instanceof ProcessNode) {
			// process part
			part = new ProcessPart();
		} else if (model instanceof ConnectionLink) {
			// process link part
			part = new ConnectionLinkEditPart();
		} else if (model instanceof ExchangeNode) {
			// exchange part
			part = new ExchangePart();
		} else if (model instanceof DummyNode) {
			// dummy part
			part = new DummyPart();
		} else if (model instanceof ExchangeContainerNode) {
			// exchange container part
			part = new ExchangeContainerPart();
		}
		if (part != null) {
			// set model
			part.setModel(model);
		}
		return part;
	}

}
