package org.openlca.app.db;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.UI;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabasePropertiesDialog extends FormDialog {

	private DatabaseConfig config;

	public DatabasePropertiesDialog(DatabaseConfig config) {
		super(UI.shell());
		this.config = config;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = UI.header(managedForm, M.Properties);
		Composite body = UI.body(form, toolkit);
		Composite content = UI.composite(body, toolkit);
		UI.gridLayout(content, 2);
		if (config instanceof DerbyConfig) {
			DerbyConfig derbyConfig = (DerbyConfig) config;
			renderDerbyConfig(derbyConfig, content, toolkit);
		} else if (config instanceof MySqlConfig) {
			MySqlConfig mysqlConfig = (MySqlConfig) config;
			renderMysqlConfiguration(mysqlConfig, content, toolkit);
		}
	}

	private void renderMysqlConfiguration(MySqlConfig conf,
			Composite parent, FormToolkit toolkit) {
		UI.labeledText(parent, toolkit, M.Type, SWT.READ_ONLY).setText(
				M.RemoteDatabase);
		UI.labeledText(parent, toolkit, M.Name, SWT.READ_ONLY).setText(
				conf.name());
		UI.labeledText(parent, toolkit, M.Host, SWT.READ_ONLY).setText(
				conf.host());
		UI.labeledText(parent, toolkit, M.Port, SWT.READ_ONLY).setText(
				Integer.toString(conf.port()));
		UI.labeledText(parent, toolkit, M.User, SWT.READ_ONLY).setText(
				conf.user());
		boolean withPassword = Strings.notEmpty(conf.password());
		UI.labeledText(parent, toolkit, M.WithPassword, SWT.READ_ONLY)
				.setText(Boolean.toString(withPassword));
	}

	private void renderDerbyConfig(DerbyConfig conf, Composite parent,
																 FormToolkit toolkit) {
		UI.labeledText(parent, toolkit, M.Type, SWT.READ_ONLY).setText(
				M.LocalDatabase);
		UI.labeledText(parent, toolkit, M.Name, SWT.READ_ONLY).setText(
				conf.name());
		UI.label(parent, toolkit, M.Folder);
		renderFolderLink(conf, parent, toolkit);
	}

	private void renderFolderLink(DerbyConfig conf, Composite parent,
																FormToolkit toolkit) {
		File folder = DatabaseDir.getRootFolder(conf.name());
		String path = folder.toURI().toString();
		Hyperlink link = UI.hyperlink(parent, toolkit);
		link.setText(Strings.cut(path, 75));
		link.setToolTipText(path);
		Controls.onClick(link, e -> {
			try {
				Desktop.browse(path);
			} catch (Exception ex) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to open folder", ex);
			}
		});
	}
}
