/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.project;

import java.util.Calendar;
import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.ModelWizardPage;
import org.openlca.core.model.Project;
import org.openlca.core.resources.ImageType;

/**
 * Wizard page for creating a new project object
 * 
 * @author Sebastian Greve
 * 
 */
public class ProjectWizardPage extends ModelWizardPage {

	/**
	 * Creates a new project wizard page
	 */
	public ProjectWizardPage() {
		super("ProjectWizardPage");
		setTitle(Messages.Projects_WizardTitle);
		setMessage(Messages.Projects_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_PROJECT.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void createContents(final Composite container) {
		// no contents to create
	}

	@Override
	public Object[] getData() {
		final Project project = new Project(UUID.randomUUID().toString(),
				getComponentName());
		project.setCategoryId(getCategoryId());
		project.setDescription(getComponentDescription());
		project.setCreationDate(Calendar.getInstance().getTime());
		project.setLastModificationDate(Calendar.getInstance().getTime());
		return new Object[] { project };
	}

}
