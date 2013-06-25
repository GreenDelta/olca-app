/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.navigation.actions;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.openlca.core.application.navigation.CategoryElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.ModelTypeElement;
import org.openlca.core.application.views.navigator.ModelWizard;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.ui.Images;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the 'New'-wizard for a model type.
 */
public class CreateModelAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private ModelType type;

	@Override
	public boolean accept(INavigationElement element) {
		if (element instanceof ModelTypeElement) {
			ModelTypeElement e = (ModelTypeElement) element;
			category = null;
			type = (ModelType) e.getData();
			initDisplay();
			return true;
		}
		if (element instanceof CategoryElement) {
			CategoryElement e = (CategoryElement) element;
			category = (Category) e.getData();
			type = category.getModelType();
			initDisplay();
			return true;
		}
		return false;
	}

	private void initDisplay() {
		// force the display of the text and image of the current type
		setText(getText());
		setImageDescriptor(getImageDescriptor());
	}

	@Override
	public boolean accept(List<INavigationElement> elements) {
		return false;
	}

	@Override
	public void run() {
		String wizardId = getWizardId();
		try {
			IWorkbenchWizard wizard = PlatformUI.getWorkbench()
					.getNewWizardRegistry().findWizard(wizardId).createWizard();
			if (wizard instanceof ModelWizard) {
				ModelWizard modelWizard = (ModelWizard) wizard;
				modelWizard.setCategory(category);
				WizardDialog dialog = new WizardDialog(UI.shell(), modelWizard);
				dialog.open();
			}
		} catch (final CoreException e) {
			log.error("Open model wizard failed", e);
		}
	}

	private String getWizardId() {
		if (type == null)
			return null;
		switch (type) {
		case ACTOR:
			return "newActorWizard";
		case FLOW:
			return "newFlowWizard";
		case FLOW_PROPERTY:
			return "newFlowPropertyWizard";
		case IMPACT_METHOD:
			return "newImpactMethodWizard";
		case PROCESS:
			return "newProcessWizard";
		case PRODUCT_SYSTEM:
			return "newProductSystemWizard";
		case PROJECT:
			return "newProjectWizard";
		case SOURCE:
			return "newSourceWizard";
		case UNIT_GROUP:
			return "newUnitGroupWizard";
		default:
			return null;
		}
	}

	@Override
	public String getText() {
		if (type == null)
			return "Unknown?";
		switch (type) {
		case ACTOR:
			return "New actor";
		case FLOW:
			return "New flow";
		case FLOW_PROPERTY:
			return "New flow property";
		case IMPACT_METHOD:
			return "New LCIA method";
		case PROCESS:
			return "New process";
		case PRODUCT_SYSTEM:
			return "New product system";
		case PROJECT:
			return "New project";
		case SOURCE:
			return "New source";
		case UNIT_GROUP:
			return "New unit group";
		default:
			return "Unknown?";
		}
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Images.getIconDescriptor(type);
	}

}
