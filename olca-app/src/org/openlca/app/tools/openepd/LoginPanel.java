package org.openlca.app.tools.openepd;

import java.util.Optional;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
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
		credentials = Ec3.credentials();
	}

	public static LoginPanel create(Composite body, FormToolkit tk) {
		var section = new LoginPanel();
		section.render(body, tk);
		return section;
	}

	public String url() {
		return credentials.ec3Url();
	}

	private void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "EC3 Login");
		var comp = UI.sectionClient(section, tk, 2);

		// EC3 URL
		var ec3UrlText = UI.formText(comp, tk, "EC3 Endpoint");
		if (Strings.notEmpty(credentials.ec3Url())) {
			ec3UrlText.setText(credentials.ec3Url());
		}
		ec3UrlText.addModifyListener($ -> {
			credentialsChanged = true;
			credentials.ec3Url(ec3UrlText.getText());
		});

		// EPD URL
		var epdUrlText = UI.formText(comp, tk, "openEPD Endpoint");
		if (Strings.notEmpty(credentials.epdUrl())) {
			epdUrlText.setText(credentials.epdUrl());
		}
		epdUrlText.addModifyListener($ -> {
			credentialsChanged = true;
			credentials.epdUrl(epdUrlText.getText());
		});

		// user
		var userText = UI.formText(comp, tk, "User");
		if (Strings.notEmpty(credentials.user())) {
			userText.setText(credentials.user());
		}
		userText.addModifyListener($ -> {
			credentialsChanged = true;
			credentials.user(userText.getText());
		});

		// login button
		UI.filler(comp, tk);
		button = tk.createButton(comp, "", SWT.NONE);
		updateButton();
		button.setImage(Icon.CONNECT.get());
		Controls.onSelect(button, $ -> {
			if (Strings.notEmpty(credentials.token())) {
				logout();
			} else {
				login();
			}
		});

		section.setExpanded(Strings.nullOrEmpty(credentials.token()));
	}

	public Optional<Ec3Client> login() {
		if (client != null)
			return Optional.of(client);
		if (button.isDisposed())
			return Optional.empty();
		if (credentialsChanged) {
			Ec3.save(credentials);
			credentialsChanged = false;
		}
		try {
			var fromToken = Ec3Client.tryToken(credentials);
			if (fromToken.isPresent()) {
				client = fromToken.get();
				return fromToken;
			}

			var dialog = new LoginDialog(credentials);
			if (dialog.open() != Window.OK)
				return Optional.empty();

			var fromLogin = Ec3Client.tryLogin(credentials);
			if (fromLogin.isEmpty()) {
				MsgBox.error("Login failed",
					"Failed to login into the EC3 API with" +
						" the given user name and password.");
				credentials.token(null);
				client = null;
				return Optional.empty();
			}

			client = fromLogin.get();
			Ec3.save(credentials);
			return fromLogin;
		} catch (Exception e) {
			ErrorReporter.on("EC3 login failed", e);
			client = null;
			return Optional.empty();
		} finally {
			updateButton();
		}
	}

	public void logout() {
		try {
			credentials.token(null);
			Ec3.save(credentials);
			if (client != null) {
				client.logout();
				client = null;
			}
		} catch (Exception e) {
			ErrorReporter.on("EC3 logout failed", e);
		} finally {
			client = null;
			updateButton();
		}
	}

	private void updateButton() {
		if (button == null || button.isDisposed())
			return;
		var label = Strings.notEmpty(credentials.token())
			? "Logout"
			: "Login";
		var tooltip = Strings.notEmpty(credentials.token())
			? "Delete the current access token"
			: "Get a new access token";
		button.setText(label);
		button.setToolTipText(tooltip);
		button.getParent().layout();
		button.getParent().redraw();
	}

	static class LoginDialog extends FormDialog {

		private final Ec3Credentials credentials;

		LoginDialog(Ec3Credentials credentials) {
			super(UI.shell());
			this.credentials = credentials;
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Login with your EC3 Account");
		}

		@Override
		protected Point getInitialSize() {
			return UI.initialSizeOf(this, 450, 250);
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var tk = mForm.getToolkit();
			var body = UI.formBody(mForm.getForm(), tk);
			var outer = tk.createComposite(body);
			UI.fillHorizontal(outer);
			UI.gridLayout(outer, 2);
			tk.createLabel(outer, "").setImage(Icon.EC3_WIZARD.get());
			var right = tk.createComposite(outer);
			UI.fillHorizontal(right);
			UI.gridLayout(right, 2, 10, 0);

			var userTxt = UI.formText(right, tk, M.User);
			UI.fillHorizontal(userTxt);
			Controls.set(userTxt, credentials.user(), credentials::user);
			var pwTxt = UI.formText(right, tk, M.Password, SWT.PASSWORD | SWT.BORDER);
			UI.fillHorizontal(pwTxt);
			pwTxt.addModifyListener($ -> credentials.password(pwTxt.getText()));
		}
	}
}
