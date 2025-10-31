package org.openlca.app.collaboration.dialogs;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.collaboration.navigation.ServerConfigurations;
import org.openlca.app.collaboration.navigation.ServerConfigurations.ServerConfig;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.util.UI;
import org.openlca.collaboration.model.Repository;
import org.openlca.util.Strings;

public class RepositorySelect extends Composite {

	private final IManagedForm form;
	private final FormToolkit toolkit;
	private Runnable onChange;
	private String repositoryId;
	ServerConfig server;

	RepositorySelect(Composite parent, IManagedForm form) {
		super(parent, SWT.NONE);
		this.form = form;
		this.toolkit = form != null ? form.getToolkit() : null;
		UI.gridLayout(this, 1, 0, 0);
		UI.gridData(this, true, false);
	}

	RepositorySelect onChange(Runnable onChange) {
		this.onChange = onChange;
		return this;
	}

	void render() {
		var group = UI.group(this, toolkit);
		group.setText(M.Location);
		UI.gridLayout(group, 1, 0, 0);
		UI.gridData(group, true, false);
		var container = UI.composite(group, toolkit);
		UI.gridLayout(container, 2);
		UI.gridData(container, true, false);
		var servers = ServerConfigurations.get();
		var serverCombo = UI.labeledCombo(container, toolkit, M.Server);
		serverCombo.setItems(servers.stream()
				.map(ServerConfig::url)
				.toArray(size -> new String[size]));
		var repositoryCombo = UI.labeledCombo(container, toolkit, M.Repository);
		serverCombo.addModifyListener(e -> {
			server = servers.get(serverCombo.getSelectionIndex());
			var repositories = WebRequests.execute(
					server.createClient()::listRepositories,
					new ArrayList<Repository>());
			repositoryCombo.setItems(repositories.stream()
				.map(repo -> repo.group() + "/" + repo.name())
				.sorted(Strings::compareIgnoreCase)
				.toArray(size -> new String[size]));
			if (onChange != null) {
				onChange.run();
			}
			if (form != null) {
				form.getForm().reflow(true);
			}
			getShell().pack(true);
		});
		repositoryCombo.addModifyListener(e -> {
			var selected = repositoryCombo.getSelectionIndex();
			repositoryId = selected != -1
					? repositoryCombo.getItem(selected)
					: null;
			if (onChange != null) {
				onChange.run();
			}
		});
	}

	String url() {
		return server.url() + "/" + repositoryId;
	}

}