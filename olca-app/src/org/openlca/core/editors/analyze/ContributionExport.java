package org.openlca.core.editors.analyze;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.io.Excel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ContributionExport implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ContributionExportData data;

	public ContributionExport(ContributionExportData data) {
		this.data = data;
	}

	@Override
	public void run() {
		if (!valid(data))
			return;
		File file = data.getFile();
		log.trace("write contribution data to file {}", file);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			HSSFWorkbook workbook = createWorkbook();
			workbook.write(fos);
		} catch (Exception e) {
			log.error("Failed to export contributions", e);
		}
	}

	private HSSFWorkbook createWorkbook() {
		HSSFWorkbook workbook = new HSSFWorkbook();
		CellStyle headerStyle = Excel.headerStyle(workbook);
		HSSFSheet sheet = workbook.createSheet(data.getTitle());
		int rowNum = 0;
		HSSFRow row = sheet.createRow(rowNum++);
		Excel.cell(row, 0, data.getItemName()).setCellStyle(headerStyle);
		Excel.cell(row, 1, data.getSelectedItem());
		row = sheet.createRow(rowNum++);
		Excel.cell(row, 0, Messages.Common_OrderBy).setCellStyle(headerStyle);
		Excel.cell(row, 1, data.getOrderType());
		row = sheet.createRow(rowNum++);
		Excel.cell(row, 0, Messages.Common_CutOff).setCellStyle(headerStyle);
		Excel.cell(row, 1, data.getCutoff());
		rowNum++;
		row = sheet.createRow(rowNum++);
		writeHeaders(row, headerStyle);
		writeData(rowNum, sheet);
		for (int i = 0; i < 5; i++)
			sheet.autoSizeColumn(i);
		return workbook;
	}

	private boolean valid(ContributionExportData data) {
		return data != null && data.getFile() != null
				&& data.getItems() != null;
	}

	private void writeHeaders(HSSFRow row, CellStyle style) {
		Excel.cell(row, 0, Messages.Analyze_Contribution).setCellStyle(style);
		Excel.cell(row, 1, Messages.Common_Process).setCellStyle(style);
		Excel.cell(row, 2, Messages.Analyze_TotalAmount).setCellStyle(style);
		Excel.cell(row, 3, Messages.Analyze_SingleAmount).setCellStyle(style);
		Excel.cell(row, 4, Messages.Common_Unit).setCellStyle(style);
	}

	private void writeData(int rowNum, HSSFSheet sheet) {
		for (ProcessContributionItem item : data.getItems()) {
			HSSFRow row = sheet.createRow(rowNum++);
			Excel.cell(row, 0, item.getContribution() * 100);
			Excel.cell(row, 1, item.getProcessName());
			Excel.cell(row, 2, item.getTotalAmount());
			Excel.cell(row, 3, item.getSingleAmount());
			Excel.cell(row, 4, item.getUnit());
		}

	}
}
