/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.views.navigator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.model.Category;
import org.openlca.core.resources.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract new wizard for model components
 */
public abstract class ModelWizard extends Wizard implements INewWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private String title;
	private ModelWizardPage wizardPage;
	private Category category;

	public ModelWizard(String title, ModelWizardPage wizardPage) {
		this.title = title;
		this.wizardPage = wizardPage;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	@Override
	public void addPages() {
		addPage(wizardPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(title);
		setDefaultPageImageDescriptor(ImageType.NEW_WIZARD.getDescriptor());
	}

	@Override
	public boolean performFinish() {
		try {
			// TODO: save model and open the editor
			return true;
		} catch (Exception e) {
			log.error("Failed to create model", e);
			return true;
		}
	}

}
