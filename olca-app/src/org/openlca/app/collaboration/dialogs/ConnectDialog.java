package org.openlca.app.collaboration.dialogs;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class ConnectDialog extends FormDialog {

	private String url;
	private AuthenticationGroup authentication;

	public ConnectDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform, "Connect Git repository");
		var formBody = form.getBody();
		createHeader(formBody, mform.getToolkit());
		var body = new Composite(formBody, SWT.NONE);
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true).widthHint = 500;
		createLocationGroup(body);
		authentication = AuthenticationGroup.create(body, this::updateButtons);
		form.reflow(true);
	}

	private void createHeader(Composite formBody, FormToolkit toolkit) {
		UI.gridLayout(formBody, 1, 0, 0);
		toolkit.paintBordersFor(formBody);
		UI.gridData(formBody, true, false);
		var descriptionComposite = toolkit.createComposite(formBody);
		UI.gridLayout(descriptionComposite, 1).marginTop = 0;
		UI.gridData(descriptionComposite, true, false);
		UI.formLabel(descriptionComposite, toolkit, "Enter the location of the Git repository.");
		var separator = new Label(formBody, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void createLocationGroup(Composite parent) {
		var group = new Group(parent, SWT.NONE);
		group.setText("Location");
		UI.gridLayout(group, 2);
		UI.gridData(group, true, false);
		var urlText = UI.formText(group, "URL:");
		var protocolText = UI.formText(group, "Protocol:");
		protocolText.setEditable(false);
		var hostText = UI.formText(group, "Host:");
		hostText.setEditable(false);
		var portText = UI.formText(group, "Port:");
		portText.setEditable(false);
		var pathText = UI.formText(group, "Repository path:");
		pathText.setEditable(false);
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
				&& !Strings.nullOrEmpty(authentication.user())
				&& !Strings.nullOrEmpty(authentication.password());
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
		return url;
	}

	public AuthenticationGroup authentication() {
		return authentication;
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

	}

}
