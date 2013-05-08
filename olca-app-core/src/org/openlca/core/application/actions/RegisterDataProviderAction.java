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

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.INewWizard;
import org.openlca.ui.UI;

/**
 * This actions opens the specified new wizard
 * 
 * @author Sebastian Greve
 * 
 */
public class RegisterDataProviderAction extends NavigationAction {

	/**
	 * The text of the action
	 */
	private final String text;

	/**
	 * The wizard to open
	 */
	private final INewWizard wizard;

	/**
	 * Creates a new instance
	 * 
	 * @param wizard
	 *            The wizard to open
	 * @param text
	 *            The text of the action
	 */
	public RegisterDataProviderAction(final INewWizard wizard, final String text) {
		this.wizard = wizard;
		this.text = text;
	}

	@Override
	protected String getTaskName() {
		return null;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public void task() {
		final WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();
	}

}
