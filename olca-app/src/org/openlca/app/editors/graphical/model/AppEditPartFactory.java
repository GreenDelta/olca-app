/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.model;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

public class AppEditPartFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		AbstractGraphicalEditPart part = null;
		if (model instanceof ProductSystemNode)
			part = new ProductSystemPart();
		else if (model instanceof ProcessNode)
			part = new ProcessPart();
		else if (model instanceof ExchangeNode)
			part = new ExchangePart();
		else if (model instanceof InputOutputNode)
			part = new InputOutputPart();
		else if (model instanceof ConnectionLink)
			part = new ConnectionLinkPart();

		if (part != null)
			part.setModel(model);
		return part;
	}

}
