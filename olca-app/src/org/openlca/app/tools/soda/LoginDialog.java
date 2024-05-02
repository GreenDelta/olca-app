package org.openlca.app.tools.soda;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.ilcd.io.SodaClient;
import org.openlca.util.Strings;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

class LoginDialog extends FormDialog {

	private Text urlText;
	private Button anoCheck;
	private Text userText;
	private Text pwText;
	private Connection con;
	private boolean hasEpds;

	static Optional<Connection> show() {
		var dialog = new LoginDialog();
		return dialog.open() == OK
				? Optional.ofNullable(dialog.con)
				: Optional.empty();
	}

	private LoginDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Connect to a data node");
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 600, 400);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		var comp = tk.createComposite(body);
		UI.fillHorizontal(comp);
		UI.gridLayout(comp, 2);

		var nodeCombo = SodaNodeCombo.create(comp, tk);
		nodeCombo.onSelect(node -> {
			urlText.setText(node.url());
			hasEpds = node.hasEpds();
		});

		urlText = UI.labeledText(comp, tk, "URL");
		UI.filler(comp, tk);
		urlText.setText("https://replace.this.url.to/Node");
		anoCheck = tk.createButton(comp, "Anonymous access", SWT.CHECK);
		anoCheck.setSelection(true);
		userText = UI.labeledText(comp, tk, "User");
		userText.setText("anonymous");
		userText.setEnabled(false);
		pwText = UI.labeledText(
				comp, tk, "Password", SWT.BORDER | SWT.PASSWORD);
		pwText.setEnabled(false);

		Controls.onSelect(anoCheck, $ -> {
			var anonymous = anoCheck.getSelection();
			userText.setText(anonymous ? "anonymous" : "");
			pwText.setText("");
			userText.setEnabled(!anonymous);
			pwText.setEnabled(!anonymous);
		});
	}

	@Override
	protected void okPressed() {
		var data = LoginData.of(this);
		var err = data.validate();
		if (err != null) {
			MsgBox.error("Invalid login data", err);
			return;
		}

		var con = App.exec("Connect to node ...", data::login);
		if (con.hasError()) {
			MsgBox.error(
					"Connection failed",
					"Failed to connect to node: " + con.error());
			return;
		}
		this.con = con;
		super.okPressed();
	}

	private record LoginData(
			String url,
			boolean anonymous,
			String user,
			String password,
			boolean hasEpds) {

		static LoginData of(LoginDialog d) {
			return new LoginData(
					d.urlText.getText().strip(),
					d.anoCheck.getSelection(),
					d.userText.getText().strip(),
					d.pwText.getText().strip(),
					d.hasEpds);
		}

		String validate() {
			if (Strings.nullOrEmpty(url))
				return "No URL provided";
			if (!url.startsWith("http://") && !url.startsWith("https://"))
				return "URL should start with http:// or https://";
			try {
				new URL(url);
			} catch (MalformedURLException e) {
				return "Invalid URL: " + e.getMessage();
			}

			if (anonymous)
				return null;

			if (Strings.nullOrEmpty(user))
				return "No user name provided";
			if (Strings.nullOrEmpty(password))
				return "No password provided";

			return null;
		}

		Connection login() {
			var url = this.url;
			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			if (!url.endsWith("/resource")) {
				url += "/resource";
			}

			try {
				var client = SodaClient.of(url);
				if (!anonymous) {
					client.login(user, password);
				}
				var stocks = client.getDataStockList().getDataStocks();
				return new Connection(client, stocks, url, user, hasEpds, null);
			} catch (Exception e) {
				return Connection.error(e.getMessage());
			}
		}
	}

}
