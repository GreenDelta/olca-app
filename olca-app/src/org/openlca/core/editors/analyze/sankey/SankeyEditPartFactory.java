/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.analyze.sankey;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

/**
 * Creates the respective edit parts for the product system, processes, and
 * process links in the Sankey diagram.
 */
public class SankeyEditPartFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		EditPart part = createEditPart(model);
		if (part != null)
			part.setModel(model);
		return part;
	}

	private EditPart createEditPart(Object model) {
		if (model instanceof ProductSystemNode)
			return new ProductSystemEditPart();
		if (model instanceof ProcessNode)
			return new ProcessEditPart();
		if (model instanceof ConnectionLink)
			return new ConnectionLinkEditPart();
		return null;
	}
}
