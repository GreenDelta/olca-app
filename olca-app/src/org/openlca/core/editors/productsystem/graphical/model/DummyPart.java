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

import java.beans.PropertyChangeEvent;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;

/**
 * EditPart for a {@link DummyNode}.
 * 
 * @see AppAbstractEditPart
 * 
 * @author Sebastian Greve
 * 
 */
public class DummyPart extends AppAbstractEditPart {

	@Override
	protected void createEditPolicies() {

	}

	@Override
	protected IFigure createFigure() {
		final IFigure figure = new Figure();
		return figure;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent arg0) {

	}

}
