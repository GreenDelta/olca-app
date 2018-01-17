package org.openlca.app.wizards.io;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.jsonld.input.UpdateMode;

/**
 * Contains settings for the JSON-LD import.
 */
class JsonImportPage extends WizardPage {

	private UpdateMode[] mods = {
			UpdateMode.NEVER,
			UpdateMode.IF_NEWER,
			UpdateMode.ALWAYS
	};

	UpdateMode updateMode = UpdateMode.NEVER;

	JsonImportPage() {
		super("JsonImportPage");
		setTitle("Import settings");
		setDescription("Select import settings for the JSON-LD import");
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1);
		Group group = new Group(body, SWT.NONE);
		group.setText("Update mode");
		UI.gridData(group, true, false);
		UI.gridLayout(group, 1);
		for (UpdateMode mode : mods) {
			Button option = new Button(group, SWT.RADIO);
			option.setText(getText(mode));
			option.setSelection(mode == updateMode);
			Controls.onSelect(option, (e) -> {
				updateMode = mode;
			});
		}
		setControl(body);
	}

	private String getText(UpdateMode mode) {
		switch (mode) {
		case NEVER:
			return "Never update a data set that already exists";
		case IF_NEWER:
			return "Update data sets with newer versions";
		case ALWAYS:
			return "Overwrite all existing data sets";
		default:
			return "?";
		}
	}

}
