package org.openlca.app.collaboration.dialogs;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class LocationGroup {

	private boolean withRepository;
	private Runnable onChange;
	private String url;

	public LocationGroup withRepository() {
		this.withRepository = true;
		return this;
	}

	public LocationGroup onChange(Runnable onChange) {
		this.onChange = onChange;
		return this;
	}

	public void render(Composite parent, FormToolkit tk) {
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
		Text pText = null;
		if (withRepository) {
			pText = UI.labeledText(group, tk, M.RepositoryPath);
			pText.setEnabled(false);
		}
		var pathText = pText;
		urlText.addModifyListener(e -> {
			var text = urlText.getText();
			var url = new UrlParts(text);
			this.url = url.isValid() ? text : null;
			protocolText.setText(url.protocol);
			hostText.setText(url.host);
			portText.setText(url.port);
			if (withRepository) {
				pathText.setText(url.path);
			}
			if (onChange != null) {
				onChange.run();
			}
		});
	}

	public String url() {
		return new UrlParts(url).toString();
	}

	private class UrlParts {

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
			this.host = withRepository
					? host
					: host + "/" + path;
			this.port = port;
			this.path = withRepository
					? path
					: "";
		}

		private boolean isValid() {
			if (Strings.nullOrEmpty(protocol))
				return false;
			if (Strings.nullOrEmpty(host))
				return false;
			if (Strings.nullOrEmpty(port))
				return false;
			if (withRepository && Strings.nullOrEmpty(path))
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
