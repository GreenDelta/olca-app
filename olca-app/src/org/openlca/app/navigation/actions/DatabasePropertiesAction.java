package org.openlca.app.navigation.actions;

import java.awt.Desktop;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.db.MySQLConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows the database properties in a window.
 */
public class DatabasePropertiesAction extends Action implements
		INavigationAction {

	private IDatabaseConfiguration config;

	public DatabasePropertiesAction() {
		setText(Messages.Properties);
		setImageDescriptor(ImageType.INFO_ICON.getDescriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement dbElement = (DatabaseElement) element;
		config = dbElement.getContent();
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		if (config == null)
			return;
		new Window().open();
	}

	private class Window extends FormDialog {

		public Window() {
			super(UI.shell());
			setText(Messages.Properties);
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			FormToolkit toolkit = managedForm.getToolkit();
			ScrolledForm form = UI.formHeader(managedForm, Messages.Properties);
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
			UI.formText(parent, toolkit, "Type", SWT.READ_ONLY).setText(
					"Remote database");
			UI.formText(parent, toolkit, "Name", SWT.READ_ONLY).setText(
					conf.getName());
			UI.formText(parent, toolkit, "Host", SWT.READ_ONLY).setText(
					conf.getHost());
			UI.formText(parent, toolkit, "Port", SWT.READ_ONLY).setText(
					Integer.toString(conf.getPort()));
			UI.formText(parent, toolkit, "User", SWT.READ_ONLY).setText(
					conf.getUser());
			boolean withPassword = Strings.notEmpty(conf.getPassword());
			UI.formText(parent, toolkit, "With password", SWT.READ_ONLY)
					.setText(Boolean.toString(withPassword));
		}

		private void renderDerbyConfig(final DerbyConfiguration conf,
				Composite parent, FormToolkit toolkit) {
			UI.formText(parent, toolkit, "Type", SWT.READ_ONLY).setText(
					"Local database");
			UI.formText(parent, toolkit, "Name", SWT.READ_ONLY).setText(
					conf.getName());
			UI.formLabel(parent, toolkit, "Folder");
			if (conf.getFolder() != null)
				renderFolderLink(conf, parent, toolkit);
		}

		private void renderFolderLink(final DerbyConfiguration conf,
				Composite parent, FormToolkit toolkit) {
			Hyperlink link = new Hyperlink(parent, SWT.NONE);
			toolkit.adapt(link);
			link.setText(Strings.cut(conf.getFolder().getAbsolutePath(), 75));
			link.setToolTipText(conf.getFolder().getAbsolutePath());
			link.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					try {
						Desktop.getDesktop().open(conf.getFolder());
					} catch (Exception ex) {
						Logger log = LoggerFactory.getLogger(getClass());
						log.error("failed to open folder", ex);
					}
				}
			});
		}
	}
}
