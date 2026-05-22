package org.openlca.app.wizards.io;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.io.ecospold1.output.EcoSpold1Export;
import org.openlca.io.ecospold1.output.EcoSpold1Export.EcoSpold1Config;

class Es1ExportConfigPage extends WizardPage {

	private final EcoSpold1Config config;

	private boolean singleFile = false;
	private boolean withDefaults = false;
	private boolean writeRefIdInfo = false;
	private boolean withLocationSuffixes = true;
	private boolean withTypeSuffixes = true;
	private boolean withProcessSuffixes = true;

	Es1ExportConfigPage() {
		super("Es1ExportConfigPage");
		setTitle(M.EcoSpoldConfiguration);
		setDescription(M.ConfigureEcospoldMessage);
		config = EcoSpold1Export.of(org.openlca.app.db.Database.get());
		config.writeSingleFile(singleFile);
		config.writeDefaultValues(withDefaults);
		config.writeRefIdInfo(writeRefIdInfo);
		config.withLocationSuffixes(withLocationSuffixes);
		config.withTypeSuffixes(withTypeSuffixes);
		config.withProcessSuffixes(withProcessSuffixes);
	}

	public EcoSpold1Config getConfig() {
		return config;
	}

	@Override
	public void createControl(Composite parent) {
		var body = UI.composite(parent);
		setControl(body);
		UI.gridLayout(body, 1);

		var generalGroup = new Group(body, SWT.NONE);
		generalGroup.setText(M.GeneralExportSettings);
		UI.gridLayout(generalGroup, 1);
		UI.fillHorizontal(generalGroup);

		var singleCheck = UI.checkbox(generalGroup, M.ExportDataSetsInOneFile);
		singleCheck.setSelection(singleFile);
		Controls.onSelect(singleCheck, $ -> {
			singleFile = singleCheck.getSelection();
			config.writeSingleFile(singleFile);
		});

		var defaultCheck = UI.checkbox(generalGroup, M.CreateDefaultValuesForMissingFields);
		defaultCheck.setSelection(withDefaults);
		Controls.onSelect(defaultCheck, $ -> {
			withDefaults = defaultCheck.getSelection();
			config.writeDefaultValues(withDefaults);
		});

		var refIdCheck = UI.checkbox(generalGroup, "Add openLCA reference ID to general comment");
		refIdCheck.setSelection(writeRefIdInfo);
		Controls.onSelect(refIdCheck, $ -> {
			writeRefIdInfo = refIdCheck.getSelection();
			config.writeRefIdInfo(writeRefIdInfo);
		});

		// product names
		var productGroup = new Group(body, SWT.NONE);
		productGroup.setText(M.ExportedProductNames);
		UI.fillHorizontal(productGroup);
		UI.gridLayout(productGroup, 1);

		var example = UI.label(productGroup, "Example: product | process {GLO}, U");
		Runnable updateExample = () -> {
			var text = "Example: product";
			if (withProcessSuffixes) {
				text += " | process";
			}
			if (withLocationSuffixes) {
				text += " {GLO}";
			}
			if (withTypeSuffixes) {
				text += ", U";
			}
			example.setText(text);
		};
		updateExample.run();

		var processCheck = UI.checkbox(productGroup, M.AppendProcessNames);
		processCheck.setSelection(withProcessSuffixes);
		Controls.onSelect(processCheck, $ -> {
			withProcessSuffixes = processCheck.getSelection();
			config.withProcessSuffixes(withProcessSuffixes);
			updateExample.run();
		});

		var locationCheck = UI.checkbox(productGroup, M.AppendLocationCodes);
		locationCheck.setSelection(withLocationSuffixes);
		Controls.onSelect(locationCheck, $ -> {
			withLocationSuffixes = locationCheck.getSelection();
			config.withLocationSuffixes(withLocationSuffixes);
			updateExample.run();
		});

		var typeCheck = UI.checkbox(productGroup, M.AppendProcessTypes);
		typeCheck.setSelection(withTypeSuffixes);
		Controls.onSelect(typeCheck, $ -> {
			withTypeSuffixes = typeCheck.getSelection();
			config.withTypeSuffixes(withTypeSuffixes);
			updateExample.run();
		});
	}

}

