package org.openlca.app.tools.soda;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.ilcd.io.SodaClient;

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
		newShell.setText(M.ConnectToADataNode);
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
		anoCheck = tk.createButton(comp, M.AnonymousAccess, SWT.CHECK);
		anoCheck.setSelection(true);
		userText = UI.labeledText(comp, tk, M.User);
		userText.setText(M.Anonymous);
		userText.setEnabled(false);
		pwText = UI.labeledText(
				comp, tk, M.Password, SWT.BORDER | SWT.PASSWORD);
		pwText.setEnabled(false);

		Controls.onSelect(anoCheck, $ -> {
			var anonymous = anoCheck.getSelection();
			userText.setText(anonymous ? M.Anonymous : "");
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
			MsgBox.error(M.InvalidLoginData, err);
			return;
		}

		var con = App.exec(M.ConnectToNodeDots, data::login);
		if (con.hasError()) {
			MsgBox.error(M.ConnectionFailed,
					M.FailedToConnectToNode + " - " + con.error());
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
			if (Strings.isBlank(url))
				return M.NoUrlProvided;
			if (!url.startsWith("http://") && !url.startsWith("https://"))
				return M.UrlShouldStartWithHttp;
			try {
				new URI(url).toURL();
			} catch (MalformedURLException | URISyntaxException e) {
				return M.InvalidUrl + " - " + e.getMessage();
			}

			if (anonymous)
				return null;

			if (Strings.isBlank(user))
				return M.NoUserNameProvided;
			if (Strings.isBlank(password))
				return M.NoPasswordProvided;

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
