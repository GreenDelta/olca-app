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

import java.beans.PropertyChangeListener;

import org.eclipse.gef.editparts.AbstractTreeEditPart;
import org.openlca.core.model.ProductSystem;

/**
 * Implementation of {@link AbstractTreeEditPart}
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class AppAbstractTreeEditPart extends AbstractTreeEditPart
		implements PropertyChangeListener {

	@Override
	public void activate() {
		super.activate();
		if (getModel() instanceof ProductSystem) {
			((ProductSystem) getModel()).addPropertyChangeListener(this);
		}
	}

	@Override
	public void deactivate() {
		if (getModel() instanceof ProductSystem) {
			((ProductSystem) getModel()).removePropertyChangeListener(this);
		}
		super.deactivate();
	}
}
