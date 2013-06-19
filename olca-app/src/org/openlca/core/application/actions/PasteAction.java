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
import org.openlca.core.application.navigation.CategoryElement;
import org.openlca.core.application.views.navigator.CopyPasteManager;

/**
 * Action to paste cutted elements in a specific category
 * 
 * @author Sebastian Greve
 * 
 */
public class PasteAction extends Action {

	/**
	 * The navigation element of the target category
	 */
	private final CategoryElement targetElement;

	/**
	 * Creates a new instance
	 * 
	 * @param targetElement
	 *            The navigation element of the target category
	 */
	public PasteAction(final CategoryElement targetElement) {
		this.targetElement = targetElement;
	}

	@Override
	public String getText() {
		return Messages.Paste;
	}

	@Override
	public boolean isEnabled() {
		return !CopyPasteManager.getInstance().isEmpty();
	}

	@Override
	public void run() {
		CopyPasteManager.getInstance().paste(targetElement);
	}
}
