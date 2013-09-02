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

import java.beans.PropertyChangeEvent;

import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.tools.ConnectionDragCreationTool;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.policy.ProcessLinkCreatePolicy;

public class ExchangePart extends AbstractNodeEditPart<ExchangeNode> {

	@Override
	protected IFigure createFigure() {
		ExchangeFigure figure = new ExchangeFigure(getModel());
		getModel().setFigure(figure);
		String name = getModel().getName();
		figure.setText(name);
		figure.addPropertyChangeListener(this);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.NODE_ROLE, new ProcessLinkCreatePolicy());
	}

	@Override
	public DragTracker getDragTracker(Request request) {
		return new ConnectionDragCreationTool();
	}

	@Override
	public InputOutputPart getParent() {
		return (InputOutputPart) super.getParent();
	}

	@Override
	protected void refreshVisuals() {
		if (getModel().isDummy())
			return;
		if (getModel().getExchange().isInput())
			getFigure().getParent().setConstraint(getFigure(),
					new GridData(SWT.LEFT, SWT.TOP, true, false));
		else
			getFigure().getParent().setConstraint(getFigure(),
					new GridData(SWT.RIGHT, SWT.TOP, true, false));
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {

	}

}
