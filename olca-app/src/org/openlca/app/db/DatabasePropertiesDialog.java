package org.openlca.app.db;


import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
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

	private final DatabaseConfig config;
	private final int style = SWT.READ_ONLY | SWT.BORDER;

	public DatabasePropertiesDialog(DatabaseConfig config) {
		super(UI.shell());
		this.config = config;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var form = UI.header(mForm, M.Database + ": " + config.name());
		var body = UI.dialogBody(form, tk);
		var content = UI.composite(body, tk);
		UI.gridLayout(content, 2);
		if (config instanceof DerbyConfig derbyConfig) {
			renderDerbyConfig(derbyConfig, content, tk);
		} else if (config instanceof MySqlConfig mysqlConfig) {
			renderMysqlConfiguration(mysqlConfig, content, tk);
		}
	}

	private void renderMysqlConfiguration(
			MySqlConfig conf, Composite parent, FormToolkit tk
	) {
		UI.labeledText(parent, tk, M.Type, style).setText(M.RemoteDatabase);
		UI.labeledText(parent, tk, M.Name, style).setText(conf.name());
		UI.labeledText(parent, tk, M.Host, style).setText(conf.host());
		UI.labeledText(parent, tk, M.Port, style)
				.setText(Integer.toString(conf.port()));
		UI.labeledText(parent, tk, M.User, style).setText(conf.user());
		boolean withPassword = Strings.notEmpty(conf.password());
		UI.labeledText(parent, tk, M.WithPassword, style)
				.setText(withPassword ? M.Yes : M.No);
	}

	private void renderDerbyConfig(
			DerbyConfig conf, Composite parent, FormToolkit tk
	) {
		UI.labeledText(parent, tk, M.Type, style).setText(M.LocalDatabase);
		UI.labeledText(parent, tk, M.Name, style).setText(conf.name());
		UI.label(parent, tk, M.Folder);
		renderFolderLink(conf, parent, tk);
	}

	private void renderFolderLink(
			DerbyConfig conf, Composite parent, FormToolkit tk
	) {
		var folder = DatabaseDir.getRootFolder(conf.name());
		var path = folder.toURI().toString();
		var link = UI.hyperlink(parent, tk);
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
