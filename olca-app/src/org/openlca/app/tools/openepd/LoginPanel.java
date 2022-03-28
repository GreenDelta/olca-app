package org.openlca.app.tools.openepd;

import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.io.openepd.Ec3Client;
import org.openlca.io.openepd.Ec3Credentials;
import org.openlca.util.Strings;

public class LoginPanel {

	private final Ec3Credentials credentials;
	private boolean credentialsChanged;
	private Ec3Client client;
	private Button button;

	private LoginPanel() {
		credentials = Ec3Credentials.getDefault();
	}

	public static LoginPanel create(Composite body, FormToolkit tk) {
		var section = new LoginPanel();
		section.render(body, tk);
		return section;
	}

	public Ec3Credentials credentials() {
		return credentials;
	}

	private void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "EC3 Login");
		var comp = UI.sectionClient(section, tk, 2);

		// EC3 URL
		var filled = 0;
		var ec3UrlText = UI.formText(comp, tk, "EC3 Endpoint");
		if (Strings.notEmpty(credentials.ec3Url())) {
			ec3UrlText.setText(credentials.ec3Url());
			filled++;
		}
		ec3UrlText.addModifyListener($ -> {
			credentialsChanged = true;
			credentials.ec3Url(ec3UrlText.getText());
		});

		// EPD URL
		var epdUrlText = UI.formText(comp, tk, "openEPD Endpoint");
		if (Strings.notEmpty(credentials.epdUrl())) {
			epdUrlText.setText(credentials.epdUrl());
			filled++;
		}
		epdUrlText.addModifyListener($ -> {
			credentialsChanged = true;
			credentials.epdUrl(epdUrlText.getText());
		});

		// user
		var userText = UI.formText(comp, tk, "User");
		if (Strings.notEmpty(credentials.user())) {
			userText.setText(credentials.user());
			filled++;
		}
		userText.addModifyListener($ -> {
			credentialsChanged = true;
			credentials.user(userText.getText());
		});

		// password
		var pwText = UI.formText(comp, tk, "Password", SWT.PASSWORD | SWT.BORDER);
		if (Strings.notEmpty(credentials.password())) {
			pwText.setText(credentials.password());
			filled++;
		}
		pwText.addModifyListener($ -> {
			credentialsChanged = true;
			credentials.password(pwText.getText());
		});

		// login button
		UI.filler(comp, tk);
		button = tk.createButton(comp, "Login", SWT.NONE);
		button.setImage(Icon.CONNECT.get());
		Controls.onSelect(button, $ -> {
			if (client == null) {
				login();
			} else {
				logout();
			}
		});
		button.addDisposeListener($ -> {
			if (client != null) {
				client.logout();
			}
		});

		section.setExpanded(filled < 3);
	}

	public Optional<Ec3Client> login() {
		if (client != null)
			return Optional.of(client);
		if (button.isDisposed())
			return Optional.empty();
		if (credentialsChanged) {
			credentials.save();
			credentialsChanged = false;
		}
		try {
			var o = credentials.login();
			if (o.isEmpty()) {
				MsgBox.error("Login failed",
					"Failed to login into the EC3 API with" +
						" the given user name and password.");
				updateButton("Login");
				client = null;
				return Optional.empty();
			}
			client = o.get();
			updateButton("Logout");
			return o;
		} catch (Exception e) {
			ErrorReporter.on("EC3 login failed", e);
			client = null;
			updateButton("Login");
			return Optional.empty();
		}
	}

	public void logout() {
		if (button.isDisposed())
			return;
		if (client != null) {
			try {
				client.logout();
			} catch (Exception e) {
				ErrorReporter.on("EC3 logout failed", e);
			} finally {
				client = null;
			}
		}
		updateButton("Login");
	}

	private void updateButton(String text) {
		if (button.isDisposed())
			return;
		button.setText(text);
		button.getParent().layout();
		button.getParent().redraw();
	}

}
