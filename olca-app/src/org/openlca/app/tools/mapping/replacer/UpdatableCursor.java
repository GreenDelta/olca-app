package org.openlca.app.tools.mapping.replacer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;

/**
 * A method template for implementing updatable cursors.
 * 
 * see: https://db.apache.org/derby/docs/10.0/manuals/develop/develop66.html
 */
abstract class UpdatableCursor implements Runnable {

	private static final AtomicInteger seq = new AtomicInteger(0);

	final IDatabase db;
	final Stats stats = new Stats();
	/**
	 * Contains the IDs of the updated models (processes, LCIA
	 * methods/categories, product systems => see the type below).
	 */
	final Set<Long> updatedModels = new HashSet<>();

	/** The type of the updated models. */
	final ModelType type;

	UpdatableCursor(IDatabase db, ModelType type) {
		this.db = db;
		this.type = type;
	}

	/** The SQL query for selecting the records. */
	abstract String querySQL();

	/** The SQL statement for updating a record. */
	abstract String updateSQL();

	/**
	 * This method is called when the cursor moved to the next row. The update
	 * needs to be called within this method and possible errors should also be
	 * handled and logged there.
	 */
	abstract void next(ResultSet cursor, PreparedStatement update);

	@Override
	public final void run() {
		try {
			Connection con = db.createConnection();
			con.setAutoCommit(false);

			// prepare the query and cursor
			Statement query = con.createStatement();
			String name = "UPDATE_CURSOR_" + seq.incrementAndGet();
			query.setCursorName(name);
			ResultSet cursor = query.executeQuery(querySQL());

			// prepare the update statement
			String usql = updateSQL() + " WHERE CURRENT OF " + name;
			PreparedStatement update = con.prepareStatement(usql);

			// run through the table
			while (cursor.next()) {
				next(cursor, update);
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

}
