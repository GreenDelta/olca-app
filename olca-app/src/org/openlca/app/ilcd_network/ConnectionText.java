package org.openlca.app.ilcd_network;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

/**
 * A text with label and change button which shows the ILCD connection
 * parameters. When the change button is clicked the preference page opens.
 */
public class ConnectionText {

	private Text text;

	public ConnectionText(Composite parent) {
		init(parent);
	}

	private void init(Composite parent) {
		new Label(parent, SWT.NONE).setText(Messages.Connection);
		text = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
		UI.gridData(text, true, false);
		createButton(parent);
		initConnectionText();
	}

	private void createButton(Composite parent) {
		Button button = new Button(parent, SWT.NONE);
		UI.gridData(button, false, false).widthHint = 60;
		button.setText(Messages.Change);
		Controls.onSelect(button, (e) -> {
			PreferencePage.open(text.getShell());
			initConnectionText();
		});
	}

	private void initConnectionText() {
		String txt = Preference.getUser() + " @ " + Preference.getUrl();
		text.setText(txt);
	}
}
