package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.M;
import org.openlca.util.Strings;

class ClipboardText {

	/**
	 * If true, the parameters in the clipboard text should be read as input
	 * parameters, otherwise as dependent (calculated) parameters.
	 */
	final boolean forInputParameters;

	final List<String[]> rows;

	private ClipboardText(boolean forInputParameters, List<String[]> rows) {
		this.forInputParameters = forInputParameters;
		this.rows = rows;
	}

	static ClipboardText split(String text) {
		if (text == null)
			return new ClipboardText(true, Collections.emptyList());
		String[] lines = text.toString().split("\n");
		List<String[]> rows = new ArrayList<>();
		boolean forInputParameters = true;
		for (int i = 0; i < lines.length; i++) {
			String[] row = lines[i].split("\t");
			for (int k = 0; k < row.length; k++) {
				row[k] = row[k].trim();
			}
			// we determine whether it is an input or dependent parameter table
			// from the first row
			if (i == 0) {
				boolean[] header = isHeader(row);
				if (header[0]) {
					forInputParameters = header[1];
					continue;
				}
				forInputParameters = isInputParameter(row);
			}
			rows.add(row);
		}
		return new ClipboardText(forInputParameters, rows);
	}

	/**
	 * Determines whether the given fields are the header row of a parameter table.
	 * This function returns a Boolean array with exactly two values: the first
	 * value indicates whether it is a header row and the second value whether it is
	 * a header for input or dependent parameters. Thus, the second value has only a
	 * meaning when the first value is true.
	 */
	private static boolean[] isHeader(String[] fields) {
		if (fields == null || fields.length < 3)
			return new boolean[] { false, false };
		// checking the first two words should be enough
		if (Strings.nullOrEqual(fields[0], M.Name)
				&& Strings.nullOrEqual(fields[1], M.Value))
			return new boolean[] { true, true };
		if (Strings.nullOrEqual(fields[0], M.Name)
				&& Strings.nullOrEqual(fields[1], M.Formula))
			return new boolean[] { true, false };
		return new boolean[] { false, false };
	}

	/**
	 * Returns true by default and false if the given row describes a dependent
	 * parameter (the second column (formula field) is not a decimal number and the
	 * third column (value field) is a decimal number).
	 */
	private static boolean isInputParameter(String[] row) {
		if (row == null || row.length < 2)
			return true; // the default value
		if (!isDecimal(row[1]))
			return false;
		if (row.length > 2 && isDecimal(row[2]))
			return false;
		return true;
	}

	private static boolean isDecimal(String text) {
		if (text == null)
			return false;
		String s = text.replace(',', '.');
		try {
			Double.parseDouble(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
