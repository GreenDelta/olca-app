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

import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.ModelWizard;

/**
 * Wizard for creating a new project object
 * 
 * @author Sebastian Greve
 * 
 */
public class ProjectWizard extends ModelWizard {

	/**
	 * Creates a new project wizard
	 */
	public ProjectWizard() {
		super(Messages.Projects_WizardTitle, new ProjectWizardPage());
	}

}
