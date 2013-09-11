package org.openlca.core.editors.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlca.core.math.IMatrix;
import org.openlca.io.xls.Excel;

public class MatrixExcelExport {

	private static final int ROW = 0;
	private static final int COLUMN = 1;

	private ExcelHeader columnHeader;
	private ExcelHeader rowHeader;
	private IMatrix matrix;

	public void setColumnHeader(ExcelHeader columnHeader) {
		this.columnHeader = columnHeader;
	}

	public void setRowHeader(ExcelHeader rowHeader) {
		this.rowHeader = rowHeader;
	}

	public void setMatrix(IMatrix matrix) {
		this.matrix = matrix;
	}

	public void writeTo(File file) throws FileNotFoundException, IOException {
		if (!file.exists()) {
			file.createNewFile();
		}

		Workbook workbook = new XSSFWorkbook();
		writeTo(workbook);

		try (FileOutputStream fos = new FileOutputStream(file)) {
			workbook.write(fos);
		}
	}

	public Sheet writeTo(Workbook workbook) {
		Sheet sheet = workbook.createSheet("Data");

		writeHeader(workbook, sheet, COLUMN);
		writeHeader(workbook, sheet, ROW);
		writeValues(sheet);

		Excel.autoSize(sheet, 0, columnHeader.getHeaderSize() + 2);
		return sheet;
	}

	private void writeHeader(Workbook workbook, Sheet sheet, int headerType) {
		int offSet = headerType == COLUMN ? rowHeader.getHeaderSize()
				: columnHeader.getHeaderSize();
		ExcelHeader header = headerType == COLUMN ? columnHeader : rowHeader;

		CellStyle bold = Excel.headerStyle(workbook);
		for (int i = 0; i < header.getHeaderSize(); i++) {
			for (int j = 0; j <= header.getEntryCount(); j++) {
				int row = headerType == COLUMN ? i : j + offSet;
				int column = headerType == ROW ? i : j + offSet;

				if (j == 0) {
					String value = header.getHeader(i);
					Excel.cell(sheet, row, column, value).setCellStyle(bold);
				} else {
					int entryIndex = j - 1;
					int valueIndex = i;
					IExcelHeaderEntry entry = header.getEntry(entryIndex);
					String value = entry.getValue(valueIndex);
					Excel.cell(sheet, row, column, value);
				}
			}
		}
	}

	private void writeValues(Sheet sheet) {
		for (int i = 0; i < matrix.getRowDimension(); i++) {
			for (int j = 0; j < matrix.getColumnDimension(); j++) {
				int row = i + columnHeader.getHeaderSize() + 1;
				int column = j + rowHeader.getHeaderSize() + 1;

				int rowIndex = rowHeader.mapIndex(i);
				int columnIndex = columnHeader.mapIndex(j);
				double value = matrix.getEntry(rowIndex, columnIndex);

				Excel.cell(sheet, row, column, value);
			}
		}
	}
}
