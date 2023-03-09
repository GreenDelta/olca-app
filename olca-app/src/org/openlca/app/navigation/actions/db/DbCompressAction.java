package org.openlca.app.navigation.actions.db;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.database.Derby;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.util.Dirs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbCompressAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private DerbyConfig config;

	public DbCompressAction() {
		setText(M.CompressDatabase);
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var e = (DatabaseElement) first;
		var config = e.getContent();
		if (!(config instanceof DerbyConfig))
			return false;
		this.config = (DerbyConfig) config;
		return true;
	}

	@Override
	public void run() {
		if (config == null) {
			DatabaseConfig conf = Database.getActiveConfiguration();
			if (!(conf instanceof DerbyConfig))
				return;
			config = (DerbyConfig) conf;
		}
		new DbCompressionDialog().open();
	}

	private class DbCompressionDialog extends FormDialog {

		private Label afterLabel;

		public DbCompressionDialog() {
			super(UI.shell());
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			FormToolkit toolkit = mform.getToolkit();
			ScrolledForm form = UI.formHeader(mform, M.CompressDatabase, Icon.DATABASE.get());
			Composite body = UI.formBody(form, toolkit);
			UI.formLabel(body, toolkit, M.ThisWillCompressTheDatabase);
			UI.formLabel(body, toolkit, M.SizeBeforeCompression + ": " + getSize() + " MB");
			afterLabel = UI.formLabel(body, toolkit, M.SizeAfterCompression + ": -");
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			getButton(IDialogConstants.OK_ID).setText(M.Compress);
		}

		private long getSize() {
			File dir = DatabaseDir.getRootFolder(Database.get().getName());
			long byteSize = Dirs.size(dir.toPath());
			return byteSize / 1024L / 1024L;
		}

		@Override
		protected void okPressed() {
			doIt();
			afterLabel.setText(M.SizeAfterCompression + ": " + getSize() + " MB");
			afterLabel.getParent().layout();
			getButton(IDialogConstants.OK_ID).setVisible(false);
			getButton(IDialogConstants.CANCEL_ID).setText("Close");
		}

		private void doIt() {
			try {
				boolean isActive = Database.isActive(config);
				Derby db;
				if (isActive) {
					if (!Editors.closeAll())
						return;
					db = (Derby) Database.get();
				} else {
					db = (Derby) Database.activate(config);
				}
				final Derby _db = db;
				App.runWithProgress(M.CompressingDatabase, () -> compressTables(_db));
				db.close();
				if (isActive)
					Database.activate(config);
				Navigator.refresh();
				HistoryView.refresh();
				CompareView.clear();
			} catch (Exception e) {
				ErrorReporter.on("failed to compress database", e);
			}
		}

		private void compressTables(Derby db) {
			try {
				Connection con = db.createConnection();
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery("SELECT SCHEMANAME, TABLENAME FROM "
						+ "sys.sysschemas s, sys.systables t " +
						"WHERE s.schemaid = t.schemaid and t.tabletype = 'T'");
				CallableStatement cs = con.prepareCall(
						"CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)");
				while (rs.next()) {
					String schema = rs.getString(1);
					String table = rs.getString(2);
					log.info("Compress table {}.{}", schema, table);
					cs.setString(1, schema);
					cs.setString(2, table);
					cs.setInt(3, 1);
					cs.execute();
				}
				cs.close();
				rs.close();
				con.close();
			} catch (Exception e) {
				ErrorReporter.on("failed to compress database", e);
			}
		}

	}

}
