package org.openlca.app.editors.systems;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProductSystem;

/**
 * Dialog for setting calculation properties and calculate a product system
 */
class CalculationWizardDialog extends WizardDialog {

	private Button resetButton;
	private Button saveButton;

	public CalculationWizardDialog(ProductSystem productSystem) {
		super(UI.shell(), new CalculationWizard(productSystem));
	}

	private void saveDefaultValues() {
		CalculationWizardPage calculationPage = (CalculationWizardPage) getWizard()
				.getPage(CalculationWizardPage.class.getCanonicalName());
		if (calculationPage == null)
			return;
		// CalculationSettings settings = calculationPage.getSettings();
		// if (settings.getAllocationMethod() != null)
		// ApplicationProperties.PROP_DEFAULT_ALLOCATION_METHOD
		// .setValue(settings.getAllocationMethod().name());
		// else
		// ApplicationProperties.PROP_DEFAULT_ALLOCATION_METHOD.setValue(null);
		// // TODO save method and nw set
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
		saveButton = createButton(parent, -1, Messages.SaveAsDefault, false);
		saveButton.setEnabled(false);
		saveButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				saveDefaultValues();
				saveButton.setEnabled(false);
			}
		});

		// create reset button
		resetButton = createButton(parent, -2, Messages.Reset, false);
		resetButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				CalculationWizardPage calculationPage = (CalculationWizardPage) getWizard()
						.getPage(CalculationWizardPage.class.getCanonicalName());
				calculationPage.reset();
			}
		});

		super.createButtonsForButtonBar(parent);
	}

}
