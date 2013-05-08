/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Mozilla Public License v1.1
 * which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 *
 * Contributors:
 *     	GreenDeltaTC - initial API and implementation
 *		www.greendeltatc.com
 *		tel.:  +49 30 4849 6030
 *		mail:  gdtc@greendeltatc.com
 *******************************************************************************/

package org.openlca.core.database.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes the complete content of a database as insert statements to a SQL file.
 */
public class SQLScriptWriter {

	private Connection con;
	private BufferedWriter writer;
	private Logger log = LoggerFactory.getLogger(getClass());

	public void write(Connection con, File file) throws Exception {
		setUp(con, file);
		List<String> tables = getTables();
		for (String table : tables) {
			try (ResultSet rs = query("SHOW COLUMNS FROM " + table)) {
				List<String> fields = new ArrayList<>();
				while (rs.next()) {
					fields.add(rs.getString(1));
				}
				exportTable(table, fields);
			}
		}
		close();
	}

	private List<String> getTables() throws Exception, SQLException {
		List<String> tables = new ArrayList<>();
		try (ResultSet rs = query("SHOW TABLES")) {
			while (rs.next()) {
				String tableName = rs.getString(1).toLowerCase();
				if (!"openlca_version".equalsIgnoreCase(tableName))
					tables.add(tableName);
			}
		}
		return tables;
	}

	private void setUp(Connection con, File file) throws IOException {
		log.trace("set up writer for file {}", file);
		this.con = con;
		if (!file.exists())
			file.createNewFile();
		FileOutputStream fos = new FileOutputStream(file);
		OutputStreamWriter wout = new OutputStreamWriter(fos, "UTF-8");
		writer = new BufferedWriter(wout);
	}

	private void close() throws IOException, SQLException {
		log.trace("close connection and file");
		writer.flush();
		writer.close();
		con.close();
	}

	// cannot close statements => resultset will be closed too
	private ResultSet query(String sql) throws Exception {
		log.trace("execute query {}", sql);
		Statement statement = con.createStatement();
		return statement.executeQuery(sql);
	}

	private String getFieldLine(List<String> fields) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < fields.size(); i++) {
			builder.append(fields.get(i));
			if (i < fields.size() - 1) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}

	private void exportTable(String tableName, List<String> fields)
			throws Exception {
		log.trace("Export table {}", tableName);
		try (ResultSet rs = query("SELECT * FROM " + tableName)) {
			while (rs.next()) {
				String valueLine = getValueLine(rs, fields);
				writer.write("INSERT INTO ");
				writer.write(tableName);
				writer.write("(");
				writer.write(getFieldLine(fields));
				writer.write(") VALUES (");
				writer.write(valueLine);
				writer.write(");");
				writer.newLine();
			}
		}
		writer.newLine();
	}

	private String getValueLine(ResultSet rs, List<String> fields)
			throws SQLException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < fields.size(); i++) {
			Object o = rs.getObject(fields.get(i));
			if (o instanceof String || o instanceof Date) {
				builder.append("'");
				appendString(o.toString(), builder);
				builder.append("'");
			} else {
				builder.append(o);
			}
			if (i < fields.size() - 1) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}

	private void appendString(String string, StringBuilder builder) {
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '\'' || c == '\\') {
				builder.append('\\');
			}
			builder.append(c);
		}
	}

}
