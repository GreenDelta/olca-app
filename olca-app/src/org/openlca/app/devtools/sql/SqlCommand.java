package org.openlca.app.devtools.sql;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.openlca.core.database.IDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SqlCommand {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public String exec(String sql, IDatabase db) {
		if (sql == null)
			return "invalid sql statement";
		var stmt = sql.trim().toLowerCase();
		return stmt.startsWith("select ") || stmt.startsWith("show ")
				? runSelect(db, sql)
				: runUpdate(db, sql);
	}

	private String runSelect(IDatabase db, String query) {
		log.info("run select statement {}", query);
		try (var con = db.createConnection();
				 var stmt = con.createStatement();
				 var result = stmt.executeQuery(query)) {

			var table = new ArrayList<String[]>();
			var fields = getFields(result);
			table.add(fields);

			while (result.next()) {
				var row = new String[fields.length];
				table.add(row);
				for (int i = 0; i < fields.length; i++) {
					var o = result.getObject(i + 1);
					if (o != null) {
						row[i] = o.toString();
					}
				}
			}
			return new TextTable().format(table);
		} catch (Exception e) {
			return handleException(e);
		}
	}

	private String[] getFields(ResultSet result) throws Exception {
		var metaData = result.getMetaData();
		var fields = new String[metaData.getColumnCount()];
		for (int i = 0; i < fields.length; i++) {
			fields[i] = metaData.getColumnLabel(i + 1);
		}
		return fields;
	}

	private String runUpdate(IDatabase database, String stmt) {
		log.info("run update statement {}", stmt);
		try (Connection con = database.createConnection()) {
			int count = con.createStatement().executeUpdate(stmt);
			con.commit();
			log.info("{} rows updated", count);
			database.getEntityFactory().getCache().evictAll();
			return count + " rows updated";
		} catch (Exception e) {
			return handleException(e);
		}
	}

	private String handleException(Exception e) {
		StringWriter writer = new StringWriter();
		try (PrintWriter out = new PrintWriter(writer)) {
			out.println("Failed to execute query: \n");
			e.printStackTrace(out);
		}
		return writer.toString();
	}

}
