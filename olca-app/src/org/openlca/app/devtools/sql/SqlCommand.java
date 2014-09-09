package org.openlca.app.devtools.sql;

import org.openlca.core.database.IDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

class SqlCommand {

	private Logger log = LoggerFactory.getLogger(getClass());

	public String exec(String sqlStatement, IDatabase database) {
		if (sqlStatement == null)
			return "invalid sql statement";
		String stmt = sqlStatement.trim().toLowerCase();
		if (stmt.startsWith("select ") || stmt.startsWith("show "))
			return runSelect(database, sqlStatement);
		else
			return runUpdate(database, sqlStatement);
	}

	private String runSelect(IDatabase database, String query) {
		log.info("run select statement {}", query);
		try (Connection con = database.createConnection()) {
			List<String[]> table = new ArrayList<>();
			ResultSet result = con.createStatement().executeQuery(query);
			String[] fields = getFields(result);
			table.add(fields);
			while (result.next()) {
				String[] row = new String[fields.length];
				table.add(row);
				for (int i = 0; i < fields.length; i++) {
					String field = fields[i];
					Object o = result.getObject(field);
					if (o != null)
						row[i] = o.toString();
				}
			}
			result.close();
			return new TextTable().format(table);
		} catch (Exception e) {
			return handleException(e);
		}
	}

	private String[] getFields(ResultSet result) throws Exception {
		ResultSetMetaData metaData = result.getMetaData();
		String[] fields = new String[metaData.getColumnCount()];
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
