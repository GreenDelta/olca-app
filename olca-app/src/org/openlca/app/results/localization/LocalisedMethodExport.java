package org.openlca.app.results.localization;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.Query;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.io.CategoryPair;
import org.openlca.io.DisplayValues;
import org.openlca.io.xls.Excel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports a localized impact assessment method set to an Excel file.
 */
public class LocalisedMethodExport implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());

	private LocalisedImpactMethod method;
	private File file;
	private HSSFWorkbook workbook;
	private CellStyle headerStyle;
	private boolean finishedWithSuccess = false;
	private IDatabase database;
	private EntityCache cache;

	public LocalisedMethodExport(LocalisedImpactMethod method, File file,
			IDatabase database, EntityCache cache) {
		this.method = method;
		this.file = file;
		this.database = database;
		this.cache = cache;
	}

	public boolean isFinishedWithSuccess() {
		return finishedWithSuccess;
	}

	@Override
	public void run() {
		finishedWithSuccess = false;
		try {
			workbook = new HSSFWorkbook();
			headerStyle = Excel.headerStyle(workbook);
			addInfoSheet(workbook);
			addCategorySheets();
			try (FileOutputStream fos = new FileOutputStream(file)) {
				workbook.write(fos);
			}
			finishedWithSuccess = true;
		} catch (Exception e) {
			log.error("Failed to export impact method", e);
		}
	}

	private void addInfoSheet(HSSFWorkbook workbook) {
		HSSFSheet sheet = workbook.createSheet("method_info");
		ImpactMethodDescriptor methodInfo = method.getImpactMethod();
		HSSFRow row = sheet.createRow(2);
		Excel.cell(row, 2, "LCIA Method").setCellStyle(headerStyle);
		Excel.cell(row, 3, methodInfo.getName());
		row = sheet.createRow(3);
		Excel.cell(row, 2, "UUID").setCellStyle(headerStyle);
		Excel.cell(row, 3, methodInfo.getRefId());
		row = sheet.createRow(5);
		Excel.cell(row, 2, "Location").setCellStyle(headerStyle);
		Excel.cell(row, 3, "UUID").setCellStyle(headerStyle);
		Excel.cell(row, 4, "Code").setCellStyle(headerStyle);
		writeLocations(sheet);
		Excel.autoSize(sheet, 2, 3, 4);
	}

	private void writeLocations(HSSFSheet sheet) {
		try {
			String jpql = "select loc from Location loc order by loc.name";
			List<Location> locations = Query.on(database).getAll(
					Location.class, jpql);
			int nextRow = 6;
			for (Location location : locations) {
				HSSFRow row = sheet.createRow(nextRow);
				Excel.cell(row, 2, location.getName());
				Excel.cell(row, 3, location.getId());
				Excel.cell(row, 4, location.getCode());
				nextRow++;
			}
		} catch (Exception e) {
			log.error("Failed to write locations", e);
		}
	}

	private void addCategorySheets() {
		int idx = 0;
		for (LocalisedImpactCategory category : method.getImpactCategories()) {
			HSSFSheet sheet = workbook.createSheet("category_" + idx);
			HSSFRow row = sheet.createRow(1);
			Excel.cell(row, 1, "Impact category").setCellStyle(headerStyle);
			Excel.cell(row, 2, category.getImpactCategory().getName());
			row = sheet.createRow(2);
			Excel.cell(row, 1, "UUID").setCellStyle(headerStyle);
			Excel.cell(row, 2, category.getImpactCategory().getRefId());
			row = sheet.createRow(4);
			Excel.cell(row, 1, "Flow - UUID").setCellStyle(headerStyle);
			Excel.cell(row, 2, "Name").setCellStyle(headerStyle);
			Excel.cell(row, 3, "Category").setCellStyle(headerStyle);
			Excel.cell(row, 4, "Sub-category").setCellStyle(headerStyle);
			Excel.cell(row, 5, "Unit").setCellStyle(headerStyle);
			int nextCol = 6;
			for (Location location : method.getLocations()) {
				Excel.cell(row, nextCol, location.getCode()).setCellStyle(
						headerStyle);
				nextCol++;
			}
			writeFactors(sheet, category);
			idx++;
		}
	}

	private void writeFactors(HSSFSheet sheet, LocalisedImpactCategory category) {
		int nextRow = 5;
		List<LocalisedImpactFactor> factors = category.getFactors();
		Collections.sort(factors);
		for (LocalisedImpactFactor factor : factors) {
			HSSFRow row = sheet.createRow(nextRow);
			FlowDescriptor flow = factor.getFlow();
			Excel.cell(row, 1, flow.getRefId());
			Excel.cell(row, 2, flow.getName());
			CategoryPair pair = CategoryPair.create(flow, cache);
			Excel.cell(row, 3, pair.getCategory());
			Excel.cell(row, 4, pair.getSubCategory());
			Excel.cell(row, 5, DisplayValues.referenceUnit(flow, cache));
			int nextCol = 6;
			for (Location loc : method.getLocations()) {
				double val = factor.getValue(loc);
				Excel.cell(row, nextCol, val);
				nextCol++;
			}
			nextRow++;
		}
		Excel.autoSize(sheet, 1, 2, 3, 4, 5);
	}
}
