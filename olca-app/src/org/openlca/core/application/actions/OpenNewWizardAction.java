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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.ModelWizard;
import org.openlca.core.database.IDatabase;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action opens the 'New wizard' for a specific type of component
 * 
 * @author Sebastian Greve
 * 
 */
public class OpenNewWizardAction extends NavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The id of the parent category of the new object
	 */
	private final String categoryId;

	/**
	 * The database
	 */
	private final IDatabase database;

	/**
	 * The image descriptor of the wizard
	 */
	private final ImageDescriptor imageDescriptor;

	/**
	 * The text of the action
	 */
	private final String text;

	/**
	 * The id of the wizard
	 */
	private final String wizardId;

	/**
	 * Creates a new instance
	 * 
	 * @param wizardId
	 *            The id of the wizard to be opened
	 * @param categoryId
	 *            The id of the parent category of the new object
	 * @param database
	 *            The database
	 */
	public OpenNewWizardAction(final String wizardId, final String categoryId,
			final IDatabase database) {
		this.wizardId = wizardId;
		this.categoryId = categoryId;
		this.database = database;
		text = NLS.bind(Messages.OpenNewWizardAction_Text, PlatformUI
				.getWorkbench().getNewWizardRegistry().findWizard(wizardId)
				.getLabel());
		imageDescriptor = PlatformUI.getWorkbench().getNewWizardRegistry()
				.findWizard(wizardId).getImageDescriptor();
	}

	@Override
	protected String getTaskName() {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return imageDescriptor;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public void task() {
		try {
			IWorkbenchWizard wizard = PlatformUI.getWorkbench()
					.getNewWizardRegistry().findWizard(wizardId).createWizard();
			if (wizard instanceof ModelWizard) {
				ModelWizard modelWizard = (ModelWizard) wizard;
				modelWizard.setDatabase(database);
				modelWizard.setCategoryId(categoryId);
				WizardDialog dialog = new WizardDialog(UI.shell(), modelWizard);
				dialog.open();
			}
		} catch (final CoreException e) {
			log.error("Open model wizard failed", e);
		}
	}
}
