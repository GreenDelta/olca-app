package org.openlca.app.wizards.io.ecospold.v1;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.io.ecospold1.output.EcoSpold1Export;
import org.openlca.io.ecospold1.output.EcoSpold1Export.EcoSpold1Config;

class ExportConfigPage extends WizardPage {

	private final EcoSpold1Config config;

	ExportConfigPage() {
		super("Es1ExportConfigPage");
		setTitle(M.EcoSpoldConfiguration);
		setDescription(M.ConfigureEcospoldMessage);
		config = EcoSpold1Export.of(Database.get());
	}

	public EcoSpold1Config getConfig() {
		return config;
	}

	@Override
	public void createControl(Composite parent) {
		var body = UI.composite(parent);
		setControl(body);
		UI.gridLayout(body, 1);
		generalConfig(body);
		productNameConfig(body);
	}

	private void generalConfig(Composite body) {
		var g = new Group(body, SWT.NONE);
		g.setText(M.GeneralExportSettings);
		UI.gridLayout(g, 1);
		UI.fillHorizontal(g);

		var defaultCheck = UI.checkbox(g, M.CreateDefaultValuesForMissingFields);
		defaultCheck.setSelection(config.isWithDefaultValues());
		Controls.onSelect(defaultCheck, $ -> {
			config.writeDefaultValues(defaultCheck.getSelection());
		});

		var singleCheck = UI.checkbox(g, M.ExportDataSetsInOneFile);
		singleCheck.setSelection(config.isWithSingleFile());
		Controls.onSelect(singleCheck, $ -> {
			config.writeSingleFile(singleCheck.getSelection());
		});

		var refIdCheck = UI.checkbox(g,
			"Add export information with data set ID to general comment");
		refIdCheck.setSelection(config.isWithRefIdInfo());
		Controls.onSelect(refIdCheck, $ -> {
			config.writeRefIdInfo(refIdCheck.getSelection());
		});
	}

	private void productNameConfig(Composite body) {
		var g = new Group(body, SWT.NONE);
		g.setText(M.ExportedProductNames);
		UI.fillHorizontal(g);
		UI.gridLayout(g, 1);

		var example = UI.label(g, "Example: product | process {GLO}, U");
		Runnable updateExample = () -> {
			var text = "Example: product";
			if (config.isWithProcessSuffixes()) {
				text += " | process";
			}
			if (config.isWithLocationSuffixes()) {
				text += " {GLO}";
			}
			if (config.isWithTypeSuffixes()) {
				text += ", U";
			}
			example.setText(text);
			example.getParent().pack();
		};
		updateExample.run();

		var processCheck = UI.checkbox(g, M.AppendProcessNames);
		processCheck.setSelection(config.isWithProcessSuffixes());
		Controls.onSelect(processCheck, $ -> {
			config.withProcessSuffixes(processCheck.getSelection());
			updateExample.run();
		});

		var locationCheck = UI.checkbox(g, M.AppendLocationCodes);
		locationCheck.setSelection(config.isWithLocationSuffixes());
		Controls.onSelect(locationCheck, $ -> {
			config.withLocationSuffixes(locationCheck.getSelection());
			updateExample.run();
		});

		var typeCheck = UI.checkbox(g, M.AppendProcessTypes);
		typeCheck.setSelection(config.isWithTypeSuffixes());
		Controls.onSelect(typeCheck, $ -> {
			config.withTypeSuffixes(typeCheck.getSelection());
			updateExample.run();
		});
	}

}

