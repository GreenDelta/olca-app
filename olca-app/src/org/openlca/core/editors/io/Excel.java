package org.openlca.core.editors.io;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper methods for Excel exports. */
public class Excel {

	private Excel() {
	}

	public static void headerStyle(Workbook workbook, Sheet sheet, int row,
			int column) {
		Cell cell = cell(sheet, row, column);
		cell.setCellStyle(headerStyle(workbook));
	}

	public static Cell cell(Sheet sheet, int row, int column) {
		Row _row = row(sheet, row);
		return cell(_row, column);
	}

	public static CellStyle headerStyle(Workbook workbook) {
		CellStyle headerStyle = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headerStyle.setFont(font);
		return headerStyle;
	}

	public static Row row(Sheet sheet, int row) {
		Row _row = sheet.getRow(row);
		if (_row == null)
			_row = sheet.createRow(row);
		return _row;
	}

	public static Cell cell(Row row, int column) {
		Cell cell = row.getCell(column);
		if (cell == null)
			cell = row.createCell(column);
		return cell;
	}

	public static Cell cell(Sheet sheet, int row, int column, String value) {
		Row _row = row(sheet, row);
		return cell(_row, column, value);
	}

	public static Cell cell(Row row, int column, String value) {
		Cell cell = cell(row, column);
		// set a default value if NULL > otherwise auto-size fails for XSSF
		cell.setCellValue(value == null ? "" : value);
		return cell;
	}

	public static Cell cell(Sheet sheet, int row, int column, double value) {
		Row _row = row(sheet, row);
		return cell(_row, column, value);
	}

	public static Cell cell(Row row, int column, double value) {
		Cell cell = cell(row, column);
		cell.setCellValue(value);
		return cell;
	}

	/**
	 * The auto-size function has a strange behaviour when you use the SXSSF
	 * streaming API of POI. Thus it is better to not call this function in this
	 * case.
	 */
	public static void autoSize(Sheet sheet, int... columns) {
		for (int column : columns)
			sheet.autoSizeColumn(column);
	}

	/**
	 * The auto-size function has a strange behaviour when you use the SXSSF
	 * streaming API of POI. Thus it is better to not call this function in this
	 * case.
	 */
	public static void autoSize(Sheet sheet, int from, int to) {
		for (int column = from; column < to; column++)
			sheet.autoSizeColumn(column);
	}

	public static String getString(Sheet sheet, int row, int col) {
		try {
			Row _row = sheet.getRow(row);
			if (_row == null)
				return null;
			Cell cell = _row.getCell(col);
			if (cell == null)
				return null;
			return cell.getStringCellValue();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Excel.class);
			log.error("Failed to get string", e);
			return null;
		}
	}

	public static double getDouble(Sheet sheet, int row, int col) {
		try {
			Row _row = sheet.getRow(row);
			if (_row == null)
				return 0d;
			Cell cell = _row.getCell(col);
			if (cell == null)
				return 0d;
			return cell.getNumericCellValue();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Excel.class);
			log.error("Failed to get double", e);
			return 0d;
		}
	}

}
