/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.result;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.results.LCIACategoryResult;
import org.openlca.core.model.results.LCIAResult;
import org.openlca.core.model.results.LCIResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for exporting the LCI/LCIA results of a product system to an Excel
 * file
 * 
 * @author Sebastian Greve
 * 
 */
public class ExportExcelAction extends Action {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Enum value for characterization export
	 */
	private static final int CHARACTERIZATION = 0;

	/**
	 * Enum value for normalized value export
	 */
	private static final int NORMALIZATION = 1;

	/**
	 * Enum value for weighted value export
	 */
	private static final int WEIGHTING = 2;

	/**
	 * The bold and bordered cell style
	 */
	private HSSFCellStyle boldBorderedCellStyle;

	/**
	 * The bold cell style
	 */
	private HSSFCellStyle boldCellStyle = null;

	/**
	 * The bold right cell style
	 */
	private HSSFCellStyle boldRightCellStyle;

	/**
	 * The bordered cell style
	 */
	private HSSFCellStyle borderedCellStyle;

	/**
	 * The database
	 */
	private IDatabase database;
	/**
	 * Date style
	 */
	private HSSFCellStyle dateCellStyle = null;

	/**
	 * Italic style
	 */
	private HSSFCellStyle italicCellStyle = null;

	/**
	 * The LCIA result to export
	 */
	private LCIAResult lciaResult;

	/**
	 * The LCI result to export
	 */
	private LCIResult lciResult;

	/**
	 * Builds the info sheet
	 * 
	 * @param workbook
	 *            The {@link HSSFWorkbook} to create the sheet in
	 */
	private void buildInfoSheet(final HSSFWorkbook workbook) {
		final HSSFSheet infoSheet = workbook
				.createSheet(Messages.Results_Information);
		infoSheet.setColumnWidth(0, 9000);
		infoSheet.setColumnWidth(1, 30000);

		String productSystem = null;
		String product = null;
		String lciaMethod = null;
		String nwSet = null;
		if (lciResult != null) {
			productSystem = lciResult.getProductSystemName();
			product = lciResult.getTargetAmount() + " "
					+ lciResult.getUnitName() + " "
					+ lciResult.getProductName();
		} else {
			productSystem = lciaResult.getProductSystem();
			product = lciaResult.getTargetAmount() + " " + lciaResult.getUnit()
					+ " " + lciaResult.getProduct();
		}
		if (lciaResult != null) {
			lciaMethod = lciaResult.getLCIAMethod();
			nwSet = lciaResult.getNormalizationWeightingSet();
		}

		int currentRowNo = 0;

		// header
		final HSSFRow headerRow = infoSheet.createRow(currentRowNo++);
		final HSSFCell headerCell = headerRow.createCell(1);
		headerCell.setCellStyle(boldCellStyle);
		headerCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		headerCell.setCellValue(new HSSFRichTextString(
				Messages.Results_ExportTitle));

		final HSSFRow processRow = infoSheet.createRow(currentRowNo++);
		final HSSFCell nameCell = processRow.createCell(1);
		nameCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		nameCell.setCellValue(new HSSFRichTextString(productSystem));

		currentRowNo++;

		writeDateRow(infoSheet, currentRowNo++, boldCellStyle,
				Messages.Results_CreationDate + ":", new Date());

		currentRowNo++;
		currentRowNo++;

		// data set information
		writeTextRow(infoSheet, currentRowNo++, italicCellStyle,
				Messages.Results_DataSetInformation, null);
		currentRowNo++;

		// name
		writeTextRow(infoSheet, currentRowNo++, boldCellStyle,
				Messages.Common_Name + ":", productSystem);

		// product
		writeTextRow(infoSheet, currentRowNo++, boldCellStyle,
				Messages.Common_QuantitativeReference + ":", product);

		if (lciaMethod != null) {
			// lcia method
			writeTextRow(infoSheet, currentRowNo++, boldCellStyle,
					Messages.Results_LCIAMethodUsed + ":", lciaMethod);

			if (nwSet != null) {
				// normalization weighting set
				writeTextRow(infoSheet, currentRowNo++, boldCellStyle,
						Messages.Results_NormalizationWeightingSet + ":", nwSet);
			}
		}
	}

	/**
	 * Creates the LCIA sheet for the given type
	 * 
	 * @param workbook
	 *            The {@link HSSFWorkbook} to create the sheet in
	 * @param type
	 *            The type of LCIA values for which a sheet should be created
	 *            (Characterization, normalization or weighting)
	 */
	private void buildLCIASheet(final HSSFWorkbook workbook, final int type) {
		String title = null;
		switch (type) {
		case CHARACTERIZATION:
			title = Messages.Results_Characterization;
			break;
		case NORMALIZATION:
			title = Messages.Results_Normalization;
			break;
		case WEIGHTING:
			title = Messages.Results_Weighting;
			break;
		}

		final HSSFSheet lciaSheet = workbook.createSheet(title);
		lciaSheet.setColumnWidth(0, 15000);
		lciaSheet.setColumnWidth(1, 8000);
		lciaSheet.setColumnWidth(2, 8000);
		lciaSheet.setColumnWidth(3, 8000);

		int currentRowNo = 0;

		// header
		final HSSFRow headerRow = lciaSheet.createRow(currentRowNo++);
		final HSSFCell headerCell = headerRow.createCell(0);
		headerCell.setCellStyle(boldCellStyle);
		headerCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		headerCell.setCellValue(new HSSFRichTextString(title));

		final HSSFRow methodRow = lciaSheet.createRow(currentRowNo++);
		final HSSFCell nameCell = methodRow.createCell(0);
		nameCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		nameCell.setCellValue(new HSSFRichTextString(
				Messages.Results_LCIAMethodUsed + ": "
						+ lciaResult.getLCIAMethod()));

		currentRowNo++;
		currentRowNo++;

		final HSSFRow titleRow = lciaSheet.createRow(currentRowNo++);
		final HSSFCell categoryCell = titleRow.createCell(0);
		categoryCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		categoryCell.setCellStyle(boldBorderedCellStyle);
		categoryCell.setCellValue(new HSSFRichTextString(
				Messages.Results_LCIACategory));

		final HSSFCell amountCell = titleRow.createCell(1);
		amountCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		amountCell.setCellStyle(boldBorderedCellStyle);
		amountCell.setCellValue(new HSSFRichTextString(Messages.Common_Amount));

		if (type != NORMALIZATION) {
			final HSSFCell unitCell = titleRow.createCell(2);
			unitCell.setCellType(HSSFCell.CELL_TYPE_STRING);
			unitCell.setCellStyle(boldBorderedCellStyle);
			unitCell.setCellValue(new HSSFRichTextString(Messages.Results_Unit));
		}

		final HSSFCell sdCell = titleRow.createCell(type != NORMALIZATION ? 3
				: 2);
		sdCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		sdCell.setCellStyle(boldBorderedCellStyle);
		sdCell.setCellValue(new HSSFRichTextString(
				Messages.Results_StandardDeviation));

		currentRowNo++;

		for (final LCIACategoryResult result : lciaResult
				.getLCIACategoryResults()) {
			final HSSFRow entryRow = lciaSheet.createRow(currentRowNo++);
			final HSSFCell cell1 = entryRow.createCell(0);
			cell1.setCellType(HSSFCell.CELL_TYPE_STRING);
			cell1.setCellValue(new HSSFRichTextString(result.getCategory()));

			double value = result.getValue();
			if (type == NORMALIZATION) {
				value = result.getNormalizedValue();
			} else if (type == WEIGHTING) {
				value = result.getWeightedValue();
			}
			final HSSFCell cell2 = entryRow.createCell(1);
			cell2.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
			cell2.setCellValue(value);

			if (type != NORMALIZATION) {
				final HSSFCell cell3 = entryRow.createCell(2);
				cell3.setCellType(HSSFCell.CELL_TYPE_STRING);
				cell3.setCellValue(new HSSFRichTextString(
						type == CHARACTERIZATION ? result.getUnit() : result
								.getWeightingUnit()));
			}

			final HSSFCell cell4 = entryRow
					.createCell(type != NORMALIZATION ? 3 : 2);
			cell4.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
			cell4.setCellValue(result.getStandardDeviation());
		}
	}

	/**
	 * Builds the LCI sheet
	 * 
	 * @param workbook
	 *            The {@link HSSFWorkbook} to create the sheet in
	 * @throws Exception
	 */
	private void buildLCISheet(final HSSFWorkbook workbook) throws Exception {

		final HSSFSheet lciSheet = workbook
				.createSheet(Messages.Results_LCIResults);

		int currentRowNo = 0;

		// inputs
		writeTextRow(lciSheet, currentRowNo++, italicCellStyle,
				Messages.Common_Inputs, null);
		currentRowNo++;

		int currentColumnNo = 0;
		HSSFRow headerRow = lciSheet.createRow(currentRowNo++);

		HSSFCell flowCell = headerRow.createCell(currentColumnNo++);
		flowCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		flowCell.setCellStyle(boldBorderedCellStyle);
		flowCell.setCellValue(new HSSFRichTextString(Messages.Results_Flow));

		HSSFCell categoryCell = headerRow.createCell(currentColumnNo++);
		categoryCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		categoryCell.setCellStyle(boldBorderedCellStyle);
		categoryCell.setCellValue(new HSSFRichTextString(
				Messages.Common_Category));

		HSSFCell propertyCell = headerRow.createCell(currentColumnNo++);
		propertyCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		propertyCell.setCellStyle(boldBorderedCellStyle);
		propertyCell.setCellValue(new HSSFRichTextString(
				Messages.Results_FlowProperty));

		HSSFCell unitCell = headerRow.createCell(currentColumnNo++);
		unitCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		unitCell.setCellStyle(boldBorderedCellStyle);
		unitCell.setCellValue(new HSSFRichTextString(Messages.Results_Unit));

		HSSFCell amountCell = headerRow.createCell(currentColumnNo++);
		amountCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		amountCell.setCellStyle(boldBorderedCellStyle);
		amountCell.setCellValue(new HSSFRichTextString(Messages.Common_Amount));

		HSSFCell typeCell = headerRow.createCell(currentColumnNo++);
		typeCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		typeCell.setCellStyle(boldBorderedCellStyle);
		typeCell.setCellValue(new HSSFRichTextString(
				Messages.Results_ExchangeType));

		List<Exchange> sorted = sortByFlowName(lciResult.getInventory());
		for (Exchange exchange : sorted) {
			if (exchange.isInput()) {
				writeExchangeRow(lciSheet, currentRowNo++, exchange);
			}
		}

		currentRowNo++;
		currentRowNo++;

		// outputs
		writeTextRow(lciSheet, currentRowNo++, italicCellStyle,
				Messages.Common_Outputs, null);
		currentRowNo++;

		currentColumnNo = 0;
		headerRow = lciSheet.createRow(currentRowNo++);

		flowCell = headerRow.createCell(currentColumnNo++);
		flowCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		flowCell.setCellStyle(boldBorderedCellStyle);
		flowCell.setCellValue(new HSSFRichTextString(Messages.Results_Flow));

		categoryCell = headerRow.createCell(currentColumnNo++);
		categoryCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		categoryCell.setCellStyle(boldBorderedCellStyle);
		categoryCell.setCellValue(new HSSFRichTextString(
				Messages.Common_Category));

		propertyCell = headerRow.createCell(currentColumnNo++);
		propertyCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		propertyCell.setCellStyle(boldBorderedCellStyle);
		propertyCell.setCellValue(new HSSFRichTextString(
				Messages.Results_FlowProperty));

		unitCell = headerRow.createCell(currentColumnNo++);
		unitCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		unitCell.setCellStyle(boldBorderedCellStyle);
		unitCell.setCellValue(new HSSFRichTextString(Messages.Results_Unit));

		amountCell = headerRow.createCell(currentColumnNo++);
		amountCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		amountCell.setCellStyle(boldBorderedCellStyle);
		amountCell.setCellValue(new HSSFRichTextString(Messages.Common_Amount));

		typeCell = headerRow.createCell(currentColumnNo++);
		typeCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		typeCell.setCellStyle(boldBorderedCellStyle);
		typeCell.setCellValue(new HSSFRichTextString(
				Messages.Results_ExchangeType));

		for (final Exchange exchange : sorted) {
			if (!exchange.isInput()) {
				writeExchangeRow(lciSheet, currentRowNo++, exchange);
			}
		}

		for (int i = 0; i < 6; i++) {
			lciSheet.autoSizeColumn((short) i);
		}

	}

	/**
	 * Getter of the category path
	 * 
	 * 
	 * @param categoryId
	 *            The id of the category the path is requested for
	 * @return The path of the category with the given id
	 * 
	 */
	private String getCategoryString(final String categoryId) {
		String text = "";
		try {
			final Category category = database.select(Category.class,
					categoryId);
			text = category.getFullPath();
		} catch (final Exception e) {
			log.error("Reading category from db failed", e);
		}
		return text;
	}

	/**
	 * Sets up the workbook
	 * 
	 * @param workbook
	 *            The {@link HSSFWorkbook} to create the sheets in
	 */
	private void setUp(final HSSFWorkbook workbook) {

		// create bold cell style
		final HSSFFont boldFont = workbook.createFont();
		boldFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		boldCellStyle = workbook.createCellStyle();
		boldCellStyle.setFont(boldFont);

		// create bold and bordered cell style
		boldBorderedCellStyle = workbook.createCellStyle();
		boldBorderedCellStyle.setFont(boldFont);
		boldBorderedCellStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
		boldBorderedCellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
		boldBorderedCellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
		boldBorderedCellStyle.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);

		// create bordered cell style
		borderedCellStyle = workbook.createCellStyle();
		borderedCellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		borderedCellStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		borderedCellStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
		borderedCellStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);

		// create italic cell style
		final HSSFFont italicFont = workbook.createFont();
		italicFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		italicFont.setItalic(true);
		italicFont.setFontHeight((short) 250);
		italicCellStyle = workbook.createCellStyle();
		italicCellStyle.setFont(italicFont);

		// create date cell style
		dateCellStyle = workbook.createCellStyle();
		dateCellStyle.setDataFormat(HSSFDataFormat
				.getBuiltinFormat("m/d/yy h:mm"));
		dateCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);

		// create bold right aligned style
		boldRightCellStyle = workbook.createCellStyle();
		boldRightCellStyle.setFont(boldFont);
		boldRightCellStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
	}

	private List<Exchange> sortByFlowName(List<Exchange> exchanges) {
		final List<Exchange> sorted = new ArrayList<>(exchanges);
		Collections.sort(sorted, new Comparator<Exchange>() {
			@Override
			public int compare(Exchange o1, Exchange o2) {
				return o1.getFlow().getName()
						.compareToIgnoreCase(o2.getFlow().getName());
			}
		});
		return sorted;
	}

	/**
	 * Writes a date row
	 * 
	 * @param sheet
	 *            The sheet to write the row to
	 * @param rowNumber
	 *            The actual row number
	 * @param cellStyle
	 *            The cell style to use
	 * @param title
	 *            The name of the row
	 * @param date
	 *            The date to write
	 */
	private void writeDateRow(final HSSFSheet sheet, final int rowNumber,
			final HSSFCellStyle cellStyle, final String title, final Date date) {
		final HSSFRow dateRow = sheet.createRow(rowNumber);
		final HSSFCell dateTextCell = dateRow.createCell(0);
		dateTextCell.setCellStyle(cellStyle);
		dateTextCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		dateTextCell.setCellValue(new HSSFRichTextString(title));
		if (date != null) {
			final HSSFCell dateCell = dateRow.createCell(1);
			dateCell.setCellStyle(dateCellStyle);
			dateCell.setCellValue(date);
		}
	}

	/**
	 * Writes an exchange into a row
	 * 
	 * @param sheet
	 *            The sheet to write the exchange to
	 * @param rowNumber
	 *            The actual row number
	 * @param exchange
	 *            The exchange to write
	 * @throws Exception
	 */
	private void writeExchangeRow(final HSSFSheet sheet, final int rowNumber,
			final Exchange exchange) throws Exception {

		int currentColumnNo = 0;

		final HSSFRow row = sheet.createRow(rowNumber);

		// flow name column
		final HSSFCell flowCell = row.createCell(currentColumnNo++);
		flowCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		flowCell.setCellStyle(borderedCellStyle);
		flowCell.setCellValue(new HSSFRichTextString(exchange.getFlow()
				.getName()));

		// flow category
		final HSSFCell flowCategoryCell = row.createCell(currentColumnNo++);
		flowCategoryCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		flowCategoryCell.setCellStyle(borderedCellStyle);
		final String categoryText = getCategoryString(exchange.getFlow()
				.getCategoryId());
		flowCategoryCell.setCellValue(new HSSFRichTextString(categoryText));

		// flow property
		final HSSFCell flowPropertyCell = row.createCell(currentColumnNo++);
		flowPropertyCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		flowPropertyCell.setCellStyle(borderedCellStyle);
		flowPropertyCell.setCellValue(new HSSFRichTextString(exchange
				.getFlowPropertyFactor().getFlowProperty().getName()));

		// unit
		final HSSFCell unitCell = row.createCell(currentColumnNo++);
		unitCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		unitCell.setCellStyle(borderedCellStyle);
		unitCell.setCellValue(new HSSFRichTextString(exchange.getUnit()
				.getName()));

		// amount
		final HSSFCell amountCell = row.createCell(currentColumnNo++);
		amountCell.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
		amountCell.setCellStyle(borderedCellStyle);
		amountCell.setCellValue(exchange.getResultingAmount().getValue());

		// exchange type
		final HSSFCell typeCell = row.createCell(currentColumnNo++);
		typeCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		typeCell.setCellStyle(borderedCellStyle);
		typeCell.setCellValue(new HSSFRichTextString(Labels
				.flowType(exchange.getFlow())));

	}

	/**
	 * Writes a text row
	 * 
	 * @param sheet
	 *            The sheet to write the row to
	 * @param rowNumber
	 *            The actual row number
	 * @param cellStyle
	 *            The cell style to use
	 * @param title
	 *            The name of the field
	 * @param text
	 *            The text of the value cell
	 */
	private void writeTextRow(final HSSFSheet sheet, final int rowNumber,
			final HSSFCellStyle cellStyle, final String title, final String text) {
		final HSSFRow row = sheet.createRow(rowNumber);
		final HSSFCell titleCell = row.createCell(0);
		titleCell.setCellStyle(cellStyle);
		titleCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		titleCell.setCellValue(new HSSFRichTextString(title));
		if (text != null && !text.equals("")) {
			final HSSFCell textCell = row.createCell(1);
			textCell.getCellStyle().setWrapText(true);
			textCell.setCellType(HSSFCell.CELL_TYPE_STRING);
			textCell.setCellValue(new HSSFRichTextString(text));
		}
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.EXCEL_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.Common_ExportToExcel;
	}

	@Override
	public void run() {
		if (lciResult != null || lciaResult != null) {
			String productSystem = null;
			if (lciResult != null) {
				productSystem = lciResult.getProductSystemName();
			} else {
				productSystem = lciaResult.getProductSystem();
			}
			final FileDialog fileDialog = new FileDialog(UI.shell(), SWT.SAVE);
			fileDialog.setFilterExtensions(new String[] { "*.xls" }); //$NON-NLS-1$
			fileDialog.setFileName(productSystem + ".xls"); //$NON-NLS-1$
			final String path = fileDialog.open();
			if (path != null) {
				final File file = new File(path);
				boolean write = true;
				if (file.exists()) {
					write = MessageDialog.openQuestion(UI.shell(),
							Messages.Common_FileAlreadyExists,
							Messages.Common_OverwriteFileQuestion);
				}
				if (write) {
					final ProgressMonitorDialog dialog = new ProgressMonitorDialog(
							UI.shell());
					try {
						dialog.run(true, false, new IRunnableWithProgress() {

							@Override
							public void run(final IProgressMonitor monitor)
									throws InvocationTargetException,
									InterruptedException {
								try {
									monitor.beginTask(NLS.bind(
											Messages.Results_ExportTo, path),
											IProgressMonitor.UNKNOWN);
									final HSSFWorkbook workbook = new HSSFWorkbook();
									monitor.subTask(Messages.Results_SettingUp);
									setUp(workbook);
									monitor.subTask(Messages.Results_CreatingInfoSheet);
									buildInfoSheet(workbook);
									monitor.subTask(Messages.Results_CreatingLCISheet);
									if (lciResult != null) {
										buildLCISheet(workbook);
									}
									if (lciaResult != null) {
										monitor.subTask(Messages.Results_CreatingLCIASheet);
										buildLCIASheet(workbook,
												CHARACTERIZATION);
										if (lciaResult
												.getNormalizationWeightingSet() != null) {
											buildLCIASheet(workbook,
													NORMALIZATION);
											buildLCIASheet(workbook, WEIGHTING);
										}
									}
									monitor.subTask(Messages.Results_WritingIntoFile);
									try (FileOutputStream fileOut = new FileOutputStream(
											path)) {
										workbook.write(fileOut);
									}
									monitor.done();
								} catch (final Exception e) {
									log.error("Run error", e);
								}
							}
						});
					} catch (final Exception e) {
						log.error("Run error", e);
					}
				}
			}

		}
	}

	public void setResults(LCIResult lciResult, LCIAResult lciaResult,
			IDatabase database) {
		this.lciaResult = lciaResult;
		this.lciResult = lciResult;
		this.database = database;
	}
}
