/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.actor;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.ModelWizardPage;
import org.openlca.core.model.Actor;
import org.openlca.core.resources.ImageType;

/**
 * Wizard page for creating a new actor object
 * 
 * @author Sebastian Greve
 * 
 */
public class ActorWizardPage extends ModelWizardPage {

	/**
	 * Creates a new actor wizard page
	 */
	public ActorWizardPage() {
		super("ActorWizardPage");
		setTitle(Messages.Actors_WizardTitle);
		setMessage(Messages.Actors_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_ACTOR.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void createContents(final Composite container) {
	}

	@Override
	public Object[] getData() {
		final Actor actor = new Actor(UUID.randomUUID().toString(),
				getComponentName());
		actor.setCategoryId(getCategoryId());
		actor.setDescription(getComponentDescription());
		return new Object[] { actor };
	}

}
