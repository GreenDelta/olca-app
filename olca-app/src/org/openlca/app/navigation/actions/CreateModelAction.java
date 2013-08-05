/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Images;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.INewModelWizard;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the 'New'-wizard for a model type.
 */
public class CreateModelAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private ModelType type;
	private INavigationElement<?> parent;

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (element instanceof ModelTypeElement) {
			ModelTypeElement e = (ModelTypeElement) element;
			parent = e;
			category = null;
			type = e.getContent();
			initDisplay();
			return true;
		}
		if (element instanceof CategoryElement) {
			CategoryElement e = (CategoryElement) element;
			parent = e;
			category = e.getContent();
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
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		String wizardId = getWizardId();
		try {
			IWorkbenchWizard wizard = PlatformUI.getWorkbench()
					.getNewWizardRegistry().findWizard(wizardId).createWizard();
			if (wizard instanceof INewModelWizard) {
				INewModelWizard modelWizard = (INewModelWizard) wizard;
				modelWizard.setCategory(category);
			}
			WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
			dialog.open();
			Navigator.refresh(parent);
		} catch (final CoreException e) {
			log.error("Open model wizard failed", e);
		}
	}

	private String getWizardId() {
		if (type == null)
			return null;
		return "wizards.new."
				+ type.getModelClass().getSimpleName().toLowerCase();
	}

	@Override
	public String getText() {
		String prefix = "Create new ";
		if (type == null)
			return "Unknown?";
		switch (type) {
		case ACTOR:
			return prefix + "actor";
		case FLOW:
			return prefix + "flow";
		case FLOW_PROPERTY:
			return prefix + "flow property";
		case IMPACT_METHOD:
			return prefix + "impact method";
		case PROCESS:
			return prefix + "process";
		case PRODUCT_SYSTEM:
			return prefix + "product system";
		case PROJECT:
			return prefix + "project";
		case SOURCE:
			return prefix + "source";
		case UNIT_GROUP:
			return prefix + "unit group";
		default:
			return "Unknown?";
		}
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Images.getIconDescriptor(type);
	}

}
