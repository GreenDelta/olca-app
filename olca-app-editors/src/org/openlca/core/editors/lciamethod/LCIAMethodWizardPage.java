/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.editors.lciamethod;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.ModelWizardPage;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.resources.ImageType;

/**
 * Wizard page for creating a new LCIA method object
 * 
 * @author Sebastian Greve
 * 
 */
public class LCIAMethodWizardPage extends ModelWizardPage {

	/**
	 * Creates a new LCIA method wizard page
	 */
	public LCIAMethodWizardPage() {
		super("LCIAMethodWizardPage");
		setTitle(Messages.Methods_WizardTitle);
		setMessage(Messages.Methods_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_METHOD.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void createContents(final Composite container) {
		// nothing to create
	}

	@Override
	public Object[] getData() {
		final LCIAMethod lciaMethod = new LCIAMethod(UUID.randomUUID()
				.toString(), getComponentName());
		lciaMethod.setCategoryId(getCategoryId());
		lciaMethod.setDescription(getComponentDescription());
		return new Object[] { lciaMethod };
	}

}
