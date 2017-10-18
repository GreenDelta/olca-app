package org.openlca.app.navigation.actions.db;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.cloud.ui.commits.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.util.Editors;
import org.openlca.app.validation.ValidationView;
import org.openlca.core.database.derby.DerbyDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbCompressAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());
	private DerbyConfiguration config;

	public DbCompressAction() {
		setText("#Compress database");
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement dbElement = (DatabaseElement) element;
		IDatabaseConfiguration config = dbElement.getContent();
		if (!(config instanceof DerbyConfiguration))
			return false;
		else {
			this.config = (DerbyConfiguration) config;
			return true;
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		if (config == null) {
			IDatabaseConfiguration conf = Database.getActiveConfiguration();
			if (!(conf instanceof DerbyConfiguration))
				return;
			config = (DerbyConfiguration) conf;
		}
		App.runInUI("#Compress database...", () -> doIt());
	}

	private void doIt() {
		boolean isActive = Database.isActive(config);
		try {
			DerbyDatabase db = null;
			if (isActive) {
				Editors.closeAll();
				ValidationView.clear();
				db = (DerbyDatabase) Database.get();
			} else {
				db = (DerbyDatabase) Database.activate(config);
			}
			compressTables(db);
			db.close();
			if (isActive)
				Database.activate(config);
			Navigator.refresh();
			HistoryView.refresh();
		} catch (Exception e) {
			log.error("failed to compress database", e);
		}
	}

	private void compressTables(DerbyDatabase db) throws Exception {
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
	}

}
