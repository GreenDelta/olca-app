package org.openlca.app.db;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabasePropertiesDialog extends FormDialog {

	private IDatabaseConfiguration config;

	public DatabasePropertiesDialog(IDatabaseConfiguration config) {
		super(UI.shell());
		this.config = config;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = UI.formHeader(managedForm, M.Properties);
		Composite body = UI.formBody(form, toolkit);
		Composite content = UI.formComposite(body, toolkit);
		if (config instanceof DerbyConfiguration) {
			DerbyConfiguration derbyConfig = (DerbyConfiguration) config;
			renderDerbyConfig(derbyConfig, content, toolkit);
		} else if (config instanceof MySQLConfiguration) {
			MySQLConfiguration mysqlConfig = (MySQLConfiguration) config;
			renderMysqlConfiguration(mysqlConfig, content, toolkit);
		}
	}

	private void renderMysqlConfiguration(MySQLConfiguration conf,
			Composite parent, FormToolkit toolkit) {
		UI.formText(parent, toolkit, M.Type, SWT.READ_ONLY).setText(
				M.RemoteDatabase);
		UI.formText(parent, toolkit, M.Name, SWT.READ_ONLY).setText(
				conf.getName());
		UI.formText(parent, toolkit, M.Host, SWT.READ_ONLY).setText(
				conf.getHost());
		UI.formText(parent, toolkit, M.Port, SWT.READ_ONLY).setText(
				Integer.toString(conf.getPort()));
		UI.formText(parent, toolkit, M.User, SWT.READ_ONLY).setText(
				conf.getUser());
		boolean withPassword = Strings.notEmpty(conf.getPassword());
		UI.formText(parent, toolkit, M.WithPassword, SWT.READ_ONLY)
				.setText(Boolean.toString(withPassword));
	}

	private void renderDerbyConfig(DerbyConfiguration conf, Composite parent,
			FormToolkit toolkit) {
		UI.formText(parent, toolkit, M.Type, SWT.READ_ONLY).setText(
				M.LocalDatabase);
		UI.formText(parent, toolkit, M.Name, SWT.READ_ONLY).setText(
				conf.getName());
		UI.formLabel(parent, toolkit, M.Folder);
		renderFolderLink(conf, parent, toolkit);
	}

	private void renderFolderLink(DerbyConfiguration conf, Composite parent,
			FormToolkit toolkit) {
		File folder = DatabaseDir.getRootFolder(conf.getName());
		String path = folder.toURI().toString();
		Hyperlink link = new Hyperlink(parent, SWT.NONE);
		toolkit.adapt(link);
		link.setText(Strings.cut(path, 75));
		link.setToolTipText(path);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				try {
					Desktop.browse(path);
				} catch (Exception ex) {
					Logger log = LoggerFactory.getLogger(getClass());
					log.error("failed to open folder", ex);
				}
			}
		});
	}
}
