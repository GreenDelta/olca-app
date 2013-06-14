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

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;

/**
 * Figure for an {@link ExchangeContainerNode}
 * 
 * @author Sebastian Greve
 * 
 */
public class ExchangeContainerFigure extends Figure {

	/**
	 * Creates a new exchange container figure
	 */
	public ExchangeContainerFigure() {
		final GridLayout layout = new GridLayout(2, true);
		layout.horizontalSpacing = 4;
		layout.verticalSpacing = ExchangeFigure.height - 17;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayoutManager(layout);
	}

	/**
	 * Disposes the children of the figure
	 */
	public void dispose() {
		for (final Object figure : getChildren()) {
			if (figure instanceof ExchangeFigure) {
				((ExchangeFigure) figure).dispose();
			}
		}
		getChildren().clear();
	}

	/**
	 * Sets the layout of this figure
	 * 
	 * @param layout
	 *            - {@link GridData}
	 */
	public void setLayout(final Object layout) {
		getParent().setConstraint(this, layout);
	}

}
