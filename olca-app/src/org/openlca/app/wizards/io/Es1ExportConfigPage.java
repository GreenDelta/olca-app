package org.openlca.app.wizards.io;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.io.ecospold1.output.ExportConfig;

class Es1ExportConfigPage extends WizardPage {

	private ExportConfig config;

	Es1ExportConfigPage() {
		super("Es1ExportConfigPage");
		setTitle(M.EcoSpoldConfiguration);
		setDescription(M.ConfigureEcospoldMessage);
		config = new ExportConfig();
	}

	public ExportConfig getConfig() {
		return config;
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = UI.composite(parent);
		UI.gridLayout(body, 1);
		createSingleCheck(body);
		createDefaultCheck(body);
		setControl(body);

	}

	private void createDefaultCheck(Composite body) {
		Button check = new Button(body, SWT.CHECK);
		check.setText(M.CreateDefaultValuesForMissingFields);
		check.setSelection(config.isCreateDefaults());
		Controls.onSelect(check, (e) ->
				config.setCreateDefaults(check.getSelection()));
	}

	private void createSingleCheck(Composite body) {
		Button check = new Button(body, SWT.CHECK);
		check.setText(M.ExportDataSetsInOneFile);
		check.setSelection(config.isSingleFile());
		Controls.onSelect(check, (e) ->
				config.setSingleFile(check.getSelection()));
	}

}
