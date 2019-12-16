package org.openlca.app.ilcd_network;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.preferences.IoPreference;
import org.openlca.app.preferences.IoPreferencePage;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

/**
 * A text with label and change button which shows the ILCD connection
 * parameters. When the change button is clicked the preference page opens.
 */
class ConnectionText {

	private Text text;

	ConnectionText(Composite parent) {
		init(parent);
	}

	private void init(Composite parent) {
		new Label(parent, SWT.NONE).setText(M.Connection);
		text = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
		UI.gridData(text, true, false);
		createButton(parent);
		initConnectionText();
	}

	private void createButton(Composite parent) {
		Button button = new Button(parent, SWT.NONE);
		UI.gridData(button, false, false).widthHint = 60;
		button.setText(M.Change);
		Controls.onSelect(button, (e) -> {
			IoPreferencePage.open(text.getShell());
			initConnectionText();
		});
	}

	private void initConnectionText() {
		String txt = IoPreference.getIlcdUrl();
		if (!Strings.nullOrEmpty(IoPreference.getIlcdUser()))
			txt = IoPreference.getIlcdUser() + "@" + txt;
		text.setText(txt);
	}
}
