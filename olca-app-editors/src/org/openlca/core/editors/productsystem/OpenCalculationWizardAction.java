/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.openlca.core.application.Messages;
import org.openlca.core.application.wizards.ProductSystemCleaner;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.resources.ImageType;

/**
 * Opens the calculation wizard.
 */
public class OpenCalculationWizardAction extends Action {

	private ProductSystemEditor editor;

	private boolean openCalculationWizard() {
		final CalculationWizardDialog dialog = new CalculationWizardDialog(
				editor.getDatabase(),
				(ProductSystem) editor.getModelComponent());
		return dialog.open() == Window.OK;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.CALCULATE_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.Systems_CalculateButtonText;
	}

	@Override
	public void run() {
		if (editor != null) {
			ProductSystemCleaner cleaner = new ProductSystemCleaner(
					(ProductSystem) editor.getModelComponent());
			int value = cleaner.cleanUp(true);
			if (value != ProductSystemCleaner.CANCELED) {
				if (value == ProductSystemCleaner.CLEANED) {
					editor.getGraphEditor().reset(false);
				}
				openCalculationWizard();
			}
		}
	}

	public void setActiveEditor(ProductSystemEditor editor) {
		this.editor = editor;
	}

}
