package org.openlca.app.tools.openepd.panel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.tools.openepd.model.Credentials;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class CredentialsSection {

	private final Credentials credentials;

	private CredentialsSection() {
		credentials = Credentials.getDefault();
	}

	public static CredentialsSection create(Composite body, FormToolkit tk) {
		var section = new CredentialsSection();
		section.render(body, tk);
		return section;
	}

	public Credentials credentials() {
		return credentials;
	}

	private void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "EC3 Login");
		var comp = UI.sectionClient(section, tk, 2);

		// url
		var filled = 0;
		var urlText = UI.formText(comp, tk, "URL");
		if (Strings.notEmpty(credentials.url())) {
			urlText.setText(credentials.url());
			filled++;
		}
		urlText.addModifyListener(
			$ -> credentials.url(urlText.getText()));

		// user
		var userText = UI.formText(comp, tk, "User");
		if (Strings.notEmpty(credentials.user())) {
			userText.setText(credentials.user());
			filled++;
		}
		userText.addModifyListener(
			$ -> credentials.user(userText.getText()));

		// password
		var pwText = UI.formText(comp, tk, "Password", SWT.PASSWORD);
		if (Strings.notEmpty(credentials.password())) {
			pwText.setText(credentials.password());
			filled++;
		}
		pwText.addModifyListener(
			$ -> credentials.password(pwText.getText()));

		section.setExpanded(filled < 3);
	}
}
