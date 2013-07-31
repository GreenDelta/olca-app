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

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.ApplicationProperties;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ProductSystem;

/**
 * Dialog for setting calculation properties and calculate a product system
 * 
 * @author Sebastian Greve
 * 
 */
class CalculationWizardDialog extends WizardDialog {

	private IDatabase database;
	private Button resetButton;
	private Button saveButton;

	public CalculationWizardDialog(IDatabase database,
			ProductSystem productSystem) {
		super(UI.shell(), new CalculationWizard(productSystem, database));
		this.database = database;
	}

	private void resetDefaultValues() {
		ApplicationProperties.PROP_DEFAULT_LCIA_METHOD.setValue(null,
				database.getUrl());
		ApplicationProperties.PROP_DEFAULT_NORMALIZATION_WEIGHTING_SET
				.setValue(null, database.getUrl());
	}

	private void saveDefaultValues() {
		CalculationWizardPage calculationPage = (CalculationWizardPage) getWizard()
				.getPage(CalculationWizardPage.ID);
		if (calculationPage == null)
			return;
		CalculationSettings settings = calculationPage.getSettings();
		if (settings.getAllocationMethod() != null) {
			ApplicationProperties.PROP_DEFAULT_ALLOCATION_METHOD
					.setValue(settings.getAllocationMethod().name());
		} else {
			ApplicationProperties.PROP_DEFAULT_ALLOCATION_METHOD.setValue(null);
		}
		if (settings.getMethod() != null)
			ApplicationProperties.PROP_DEFAULT_LCIA_METHOD.setValue(settings
					.getMethod().getId(), database.getUrl());
		if (settings.getNwSet() != null)
			ApplicationProperties.PROP_DEFAULT_NORMALIZATION_WEIGHTING_SET
					.setValue(settings.getNwSet().getId(), database.getUrl());
	}

	@Override
	protected Button createButton(Composite parent, int id, String label,
			boolean defaultButton) {
		String l = label;
		if (id == 16) {
			l = Messages.Calculate;
		}
		return super.createButton(parent, id, l, defaultButton);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create save button
		saveButton = createButton(parent, -1, Messages.SaveDefaults, false);
		saveButton.setEnabled(false);
		saveButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no action on default selection
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				saveDefaultValues();
				saveButton.setEnabled(false);
			}
		});

		// create reset button
		resetButton = createButton(parent, -2, Messages.Reset, false);
		resetButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no action on default selection
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				resetDefaultValues();
				CalculationWizardPage calculationPage = (CalculationWizardPage) getWizard()
						.getPage(CalculationWizardPage.ID);
				calculationPage.reset();
			}
		});

		super.createButtonsForButtonBar(parent);
	}

}
