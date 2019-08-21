package org.openlca.app.tools.mapping.replacer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.openlca.core.database.IDatabase;

/**
 * A method template for implementing updatable cursors.
 * 
 * see: https://db.apache.org/derby/docs/10.0/manuals/develop/develop66.html
 */
abstract class UpdatableCursor implements Runnable {

	private static final AtomicInteger seq = new AtomicInteger(0);

	private final IDatabase db;

	UpdatableCursor(IDatabase db) {
		this.db = db;
	}

	/** The SQL query for selecting the records. */
	abstract String querySQL();

	/** The SQL statement for updating a record. */
	abstract String updateSQL();

	/**
	 * This method is called when the cursor moved to the next row. When this
	 * method returns true the update statement is called.
	 */
	abstract boolean next(ResultSet cursor, PreparedStatement update);

	@Override
	public final void run() {
		try {
			Connection con = db.createConnection();
			con.setAutoCommit(false);

			// prepare the query and cursor
			Statement query = con.createStatement();
			String name = cursorName();
			query.setCursorName(name);
			ResultSet cursor = query.executeQuery(querySQL());

			// prepare the update statement
			String usql = updateSQL() + " WHERE CURRENT OF " + name;
			PreparedStatement update = con.prepareStatement(usql);

			// run through the table
			while (cursor.next()) {
				boolean doit = next(cursor, update);
				if (doit) {
					update.executeUpdate();
				}
			}

			// free and commit
			cursor.close();
			query.close();
			update.close();
			con.commit();
			con.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String cursorName() {
		int i = seq.incrementAndGet();
		return "UPDATE_CURSOR_" + i;
	}
}
