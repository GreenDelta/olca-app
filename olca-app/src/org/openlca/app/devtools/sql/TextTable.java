package org.openlca.app.devtools.sql;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

class TextTable {

	public String format(List<String[]> table) {
		if (table == null || table.isEmpty())
			return "0 results";
		return formatTable(table);
	}

	private String formatTable(List<String[]> table) {
		StringWriter writer = new StringWriter();
		try (PrintWriter out = new PrintWriter(writer)) {
			for (final String[] row : table) {
				if (row != null)
					out.println(getRow(row));
			}
		}
		return writer.toString();
	}

	private String getRow(String[] row) {
		StringBuilder builder = new StringBuilder();
		for (String value : row) {
			String val = value == null ? "NULL" : value;
			builder.append(val).append('\t');
		}
		return builder.toString();
	}
}
