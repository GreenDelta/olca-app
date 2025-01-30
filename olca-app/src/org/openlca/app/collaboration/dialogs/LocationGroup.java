package org.openlca.app.collaboration.dialogs;

import java.net.URI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

class LocationGroup extends Composite {

	private final FormToolkit toolkit;
	private boolean withRepository;
	private boolean withStoreOption;
	private Runnable onChange;
	private UrlParts url;
	private boolean storeConnection = CollaborationPreference.storeConnection();

	LocationGroup(Composite parent) {
		this(parent, null);
	}

	LocationGroup(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		UI.gridLayout(this, 1, 0, 0);
		UI.gridData(this, true, false);
		if (toolkit != null) {
			toolkit.adapt(this);
		}
	}

	LocationGroup withRepository() {
		this.withRepository = true;
		return this;
	}

	LocationGroup withStoreOption() {
		this.withStoreOption = true;
		return this;
	}

	LocationGroup onChange(Runnable onChange) {
		this.onChange = onChange;
		return this;
	}

	void render() {
		var group = UI.group(this, toolkit);
		group.setText(M.Location);
		UI.gridLayout(group, 2);
		UI.gridData(group, true, false);
		var urlText = UI.labeledText(group, toolkit, M.URL);
		if (withStoreOption) {
			UI.label(group);
			var storeCheck = UI.checkbox(group, toolkit, "Store connection");
			storeCheck.setSelection(storeConnection);
			Controls.onSelect(storeCheck, e -> {
				storeConnection = storeCheck.getSelection();
			});
		}
		var protocolText = UI.labeledText(group, toolkit, M.Protocol);
		protocolText.setEnabled(false);
		var hostText = UI.labeledText(group, toolkit, M.Host);
		hostText.setEnabled(false);
		var contextText = UI.labeledText(group, toolkit, M.ContextPath);
		contextText.setEnabled(false);
		var portText = UI.labeledText(group, toolkit, M.Port);
		portText.setEnabled(false);
		Text pText = null;
		if (withRepository) {
			pText = UI.labeledText(group, toolkit, M.RepositoryId);
			pText.setEnabled(false);
		}
		var pathText = pText;
		urlText.addModifyListener(e -> {
			var text = urlText.getText();
			url = new UrlParts(text);
			protocolText.setText(url.protocol);
			hostText.setText(url.host);
			portText.setText(url.port);
			contextText.setText(url.context);
			if (withRepository) {
				pathText.setText(url.repositoryId);
			}
			if (onChange != null) {
				onChange.run();
			}
		});
	}

	String url() {
		return url.getUrl(true);
	}

	String serverUrl() {
		return url.getUrl(false);
	}

	boolean storeConnection() {
		return storeConnection;
	}

	private class UrlParts {

		private final String protocol;
		private final String host;
		private final String port;
		private final String context;
		private final String repositoryId;

		private UrlParts(String u) {
			var protocol = "";
			var host = "";
			var port = "";
			var context = "";
			var repositoryId = "";
			try {
				var url = URI.create(u).toURL();
				protocol = url.getProtocol();
				host = url.getHost();
				if (host != null && host.endsWith("/")) {
					host = host.substring(host.length() - 1);
				}
				if (url.getPort() != -1) {
					port = Integer.toString(url.getPort());
				} else if (protocol.equals("http")) {
					port = "80";
				} else if (protocol.equals("https")) {
					port = "443";
				}
				var path = url.getPath();
				while (!Strings.nullOrEmpty(path) && path.startsWith("/")) {
					path = path.substring(1);
				}
				while (!Strings.nullOrEmpty(path) && path.endsWith("/")) {
					path = path.substring(0, path.length() - 1);
				}
				if (!Strings.nullOrEmpty(path)) {
					if (!withRepository) {
						context = path;
					} else {
						var split = path.split("/");
						if (split.length == 1) {
							repositoryId = split[0];
						} else {
							repositoryId = split[split.length - 2] + "/" + split[split.length - 1];
							if (split.length > 2) {
								for (var i = 0; i < split.length - 2; i++) {
									if (i > 0) {
										context += "/";
									}
									context += split[i];
								}
							}
						}
					}
				}
			} catch (Exception e) {
			}
			this.protocol = protocol;
			this.host = host;
			this.port = port;
			this.context = context;
			this.repositoryId = repositoryId;
		}

		private boolean isValid() {
			if (Strings.nullOrEmpty(protocol))
				return false;
			if (Strings.nullOrEmpty(host))
				return false;
			if (Strings.nullOrEmpty(port))
				return false;
			if (!withRepository)
				return true;
			return !Strings.nullOrEmpty(repositoryId)
					&& repositoryId.contains("/")
					&& !repositoryId.startsWith("/")
					&& !repositoryId.endsWith("/");
		}

		public String getUrl(boolean withRepository) {
			if (!isValid())
				return null;
			var url = protocol + "://" + host;
			if (!port.equals("80") && !port.equals("443")) {
				url += ":" + port;
			}
			if (!Strings.nullOrEmpty(context)) {
				url += "/" + context;
			}
			if (withRepository && !Strings.nullOrEmpty(repositoryId)) {
				url += "/" + repositoryId;
			}
			return url;
		}

	}
}
