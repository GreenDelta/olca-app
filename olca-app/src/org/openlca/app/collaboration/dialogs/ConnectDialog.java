package org.openlca.app.collaboration.dialogs;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.tools.authentification.AuthenticationGroup;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class ConnectDialog extends FormDialog {

	private String url;
	private AuthenticationGroup auth = new AuthenticationGroup();
	private boolean withPassword;

	public ConnectDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	public ConnectDialog withPassword() {
		this.withPassword = true;
		return this;
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var formBody = UI.header(form, form.getToolkit(),
				"Connect Git repository",
				"Enter the location of the Git repository.");
		var body = UI.composite(formBody, form.getToolkit());
		UI.gridLayout(body,  1);
		UI.gridData(body, true, true).widthHint = 500;
		createLocationGroup(body, form.getToolkit());
		if (withPassword) {
			auth.withPassword();
		}
		auth.withUser()
				.onChange(this::updateButtons)
				.render(body, form.getToolkit(), SWT.NONE);
		form.getForm().reflow(true);
	}

	private void createLocationGroup(Composite parent, FormToolkit tk) {
		var group = UI.group(parent, tk);
		group.setText("Location");
		UI.gridLayout(group, 2);
		UI.gridData(group, true, false);
		var urlText = UI.labeledText(group, tk, M.URL);
		var protocolText = UI.labeledText(group, tk, "Protocol");
		protocolText.setEnabled(false);
		var hostText = UI.labeledText(group, tk, M.Host);
		hostText.setEnabled(false);
		var portText = UI.labeledText(group, tk, M.Port);
		portText.setEnabled(false);
		var pathText = UI.labeledText(group, tk, M.RepositoryPath);
		pathText.setEnabled(false);
		urlText.addModifyListener(e -> {
			var text = urlText.getText();
			var url = new UrlParts(text);
			this.url = url.isValid() ? text : null;
			protocolText.setText(url.protocol);
			hostText.setText(url.host);
			portText.setText(url.port);
			pathText.setText(url.path);
			updateButtons();
		});
	}

	private void updateButtons() {
		var enabled = !Strings.nullOrEmpty(url)
				&& !Strings.nullOrEmpty(auth.user())
				&& (!withPassword || !Strings.nullOrEmpty(auth.password()));
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		var commit = createButton(parent, IDialogConstants.OK_ID, M.Connect, true);
		commit.setEnabled(false);
		setButtonLayoutData(commit);
	}

	public String url() {
		return new UrlParts(url).toString();
	}

	public String user() {
		return auth.user();
	}

	public GitCredentialsProvider credentials() {
		return new GitCredentialsProvider(auth.user(), auth.password());
	}

	private static class UrlParts {

		private final String protocol;
		private final String host;
		private final String port;
		private final String path;

		private UrlParts(String u) {
			var protocol = "";
			var host = "";
			var port = "";
			var path = "";
			try {
				var url = new URL(u);
				protocol = url.getProtocol();
				host = url.getHost();
				if (url.getPort() != -1) {
					port = Integer.toString(url.getPort());
				} else if (protocol.equals("http")) {
					port = "80";
				} else if (protocol.equals("https")) {
					port = "443";
				}
				path = url.getPath();
				if (path.startsWith("/")) {
					path = path.substring(1);
				}
				if (path.endsWith("/")) {
					path = path.substring(0, path.length() - 1);
				}
			} catch (MalformedURLException e) {
			}
			this.protocol = protocol;
			this.host = host;
			this.port = port;
			this.path = path;
		}

		private boolean isValid() {
			if (Strings.nullOrEmpty(protocol))
				return false;
			if (Strings.nullOrEmpty(host))
				return false;
			if (Strings.nullOrEmpty(port))
				return false;
			if (Strings.nullOrEmpty(path))
				return false;
			return true;
		}

		@Override
		public String toString() {
			var url = protocol + "://" + host;
			if (!port.equals("80") && !port.equals("443")) {
				url += ":" + port;
			}
			url += "/" + path;
			return url;
		}

	}

}
