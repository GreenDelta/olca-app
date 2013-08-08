/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.actions;

import org.eclipse.jface.action.Action;
import org.openlca.core.application.Messages;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.views.navigator.CopyPasteManager;

/**
 * Action for cutting an element
 */
public class CutAction extends Action {

	private final INavigationElement[] elements;

	public CutAction(INavigationElement[] elements) {
		this.elements = elements;
	}

	@Override
	public String getText() {
		return Messages.Cut;
	}

	@Override
	public void run() {
		CopyPasteManager.getInstance().cut(elements);
	}

}
