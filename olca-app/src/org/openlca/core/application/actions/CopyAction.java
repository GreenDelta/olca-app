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
import org.openlca.app.Messages;
import org.openlca.app.navigation.CopyPasteManager;
import org.openlca.app.navigation.INavigationElement;

/**
 * Action for copying elements
 * 
 * @author Sebastian Greve
 * 
 */
public class CopyAction extends Action {

	/**
	 * The elements to copy
	 */
	private final INavigationElement[] elements;

	/**
	 * Creates a new instance
	 * 
	 * @param elements
	 *            The elements to copy
	 */
	public CopyAction(final INavigationElement[] elements) {
		this.elements = elements;
	}

	@Override
	public String getText() {
		return Messages.Copy;
	}

	@Override
	public void run() {
		CopyPasteManager.getInstance().copy(elements);
	}

}
