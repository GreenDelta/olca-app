package org.openlca.core.editors.io;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.openlca.core.database.IDatabase;
import org.openlca.core.editors.model.FlowInfo;
import org.openlca.core.editors.model.FlowInfoDao;
import org.openlca.core.math.FlowIndex;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports an analysis result to Excel. Because of the size of the results we
 * use the POI streaming API here (SXSSF, see
 * http://poi.apache.org/spreadsheet/how-to.html#sxssf). After a sheet is filled
 * we flush its rows which means that these rows are written to disk and not
 * accessible from memory any more.
 */
public class AnalysisResultExport {

	/** Number of attributes of the flow information. */
	final int FLOW_INFO_SIZE = 5;

	/** Number of attributes of the process information. */
	final int PROCESS_INFO_SIZE = 3;

	/** Number of attributes of the impact category information. */
	final int IMPACT_INFO_SIZE = 3;

	private Logger log = LoggerFactory.getLogger(getClass());
	private File file;
	private IDatabase database;
	private AnalysisResult result;

	private CellStyle headerStyle;
	private SXSSFWorkbook workbook;
	private List<FlowInfo> flowInfos = new ArrayList<>();
	private Map<FlowInfo, Flow> flowInfoMap = new HashMap<>();
	private Process[] processes;

	private ImpactCategoryDescriptor[] impacts;

	public AnalysisResultExport(File file, IDatabase database) {
		this.file = file;
		this.database = database;
	}

	public void run(AnalysisResult result) throws Exception {
		this.result = result;
		prepareFlowInfos();
		prepareProcesses();
		prepareImpacts();
		workbook = new SXSSFWorkbook(-1); // no default flushing (see
											// Excel.cell)!
		headerStyle = Excel.headerStyle(workbook);
		writeInventorySheets(result);
		writeImpactSheets(result);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			workbook.write(fos);
			fos.flush();
		}
	}

	private void writeInventorySheets(AnalysisResult result) {
		SXSSFSheet infoSheet = sheet("info");
		fillInfoSheet(infoSheet);
		flush(infoSheet);
		SXSSFSheet lciSheet = sheet("LCI (total)");
		fillTotalInventory(lciSheet);
		flush(lciSheet);
		SXSSFSheet lciConSheet = sheet("LCI (contributions)");
		AnalysisProcessInventories.write(lciConSheet, result, this);
		flush(lciConSheet);
	}

	private void writeImpactSheets(AnalysisResult result) {
		if (!result.hasImpactResults())
			return;
		SXSSFSheet totalImpactSheet = sheet("LCIA (total)");
		AnalysisTotalImpact.write(totalImpactSheet, result, this);
		flush(totalImpactSheet);
		SXSSFSheet singleImpactSheet = sheet("LCIA (contributions)");
		AnalysisProcessImpacts.write(singleImpactSheet, result, this);
		flush(singleImpactSheet);
		SXSSFSheet flowImpactSheet = sheet("LCIA (flows)");
		AnalysisFlowImpacts.write(flowImpactSheet, result, this);
		flush(flowImpactSheet);
	}

	private SXSSFSheet sheet(String name) {
		return (SXSSFSheet) workbook.createSheet(name);
	}

	private void flush(SXSSFSheet sheet) {
		log.trace("flush sheet {}", sheet);
		try {
			sheet.flushRows();
		} catch (Exception e) {
			log.error("Failed to flush rows");
		}
	}

	private void prepareFlowInfos() {
		FlowInfoDao dao = new FlowInfoDao(database);
		for (Flow flow : result.getFlowIndex().getFlows()) {
			FlowInfo info = dao.fromFlow(flow);
			flowInfos.add(info);
			flowInfoMap.put(info, flow);
		}
		Collections.sort(flowInfos);
	}

	private void prepareProcesses() {
		processes = result.getSetup().getProductSystem().getProcesses();
		final Process refProcess = result.getSetup().getReferenceProcess();
		if (refProcess == null)
			return;
		Arrays.sort(processes, new Comparator<Process>() {
			@Override
			public int compare(Process o1, Process o2) {
				if (refProcess.equals(o1))
					return -1;
				if (refProcess.equals(o2))
					return 1;
				return Strings.compare(o1.getName(), o2.getName());
			}
		});
	}

	private void prepareImpacts() {
		if (!result.hasImpactResults())
			return;
		impacts = result.getImpactCategories().clone();
		Arrays.sort(impacts, new Comparator<ImpactCategoryDescriptor>() {
			@Override
			public int compare(ImpactCategoryDescriptor d1,
					ImpactCategoryDescriptor d2) {
				return Strings.compare(d1.getName(), d2.getName());
			}
		});
	}

	/** Get the header style of the workbook. */
	CellStyle getHeaderStyle() {
		return headerStyle;
	}

	/** Visit the sorted flows of the analysis result. */
	void visitFlows(FlowVisitor visitor) {
		FlowIndex index = result.getFlowIndex();
		for (FlowInfo info : flowInfos) {
			Flow flow = flowInfoMap.get(info);
			if (flow == null)
				continue;
			visitor.next(flow, info, index.isInput(flow));
		}
	}

	/**
	 * Returns the sorted processes of the result, the reference process is at
	 * the first location.
	 */
	Process[] getProcesses() {
		return processes;
	}

	/** Returns the sorted impact assessment categories of the result. */
	ImpactCategoryDescriptor[] getImpacts() {
		return impacts != null ? impacts : new ImpactCategoryDescriptor[0];
	}

	private void fillInfoSheet(Sheet sheet) {
		ProductSystem system = result.getSetup().getProductSystem();
		Exchange refExchange = system.getReferenceExchange();
		header(sheet, 1, 1, "Analysis result");
		header(sheet, 2, 1, "Product system");
		Excel.cell(sheet, 2, 2, system.getName());
		header(sheet, 3, 1, "Demand - product");
		Excel.cell(sheet, 3, 2, refExchange.getFlow().getName());
		header(sheet, 4, 1, "Demand - value");
		Excel.cell(sheet, 4, 2, system.getTargetAmount() + " "
				+ system.getTargetUnit().getName());
		// Excel.autoSize(sheet, 1, 2);
	}

	private void fillTotalInventory(Sheet sheet) {
		int row = 1;
		row = writeTotalResults(sheet, row, true);
		writeTotalResults(sheet, row + 2, false);
		// Excel.autoSize(sheet, 1, 2, 3, 4, 5, 6);
	}

	private int writeTotalResults(Sheet sheet, int startRow, boolean inputs) {
		Process refProcess = result.getSetup().getReferenceProcess();
		FlowIndex flowIndex = result.getFlowIndex();
		int rowNo = startRow;
		String section = inputs ? "Inputs" : "Outputs";
		Excel.cell(sheet, rowNo++, 1, section).setCellStyle(headerStyle);
		writeFlowRowHeader(sheet, rowNo);
		Excel.cell(sheet, rowNo++, 6, "Result").setCellStyle(headerStyle);
		for (FlowInfo info : flowInfos) {
			Flow flow = flowInfoMap.get(info);
			if (flowIndex.isInput(flow) != inputs)
				continue;
			double amount = result.getResult(refProcess, flow);
			if (amount == 0)
				continue;
			writeFlowRowInfo(sheet, rowNo, info);
			Excel.cell(sheet, rowNo, 6, amount);
			rowNo++;
		}
		return rowNo;
	}

	/**
	 * Writes the process information header into the given column starting at
	 * row 1. The next free row is 1 + PROCESS_INFO_SIZE.
	 */
	void writeProcessColHeader(Sheet sheet, int col) {
		int row = 1;
		header(sheet, row++, col, "Process UUID");
		header(sheet, row++, col, "Process");
		header(sheet, row++, col, "Location");
	}

	/**
	 * Writes the process information header into the given row starting at
	 * column 1. The next free column is 1 + PROCESS_INFO_SIZE.
	 */
	void writeProcessRowHeader(Sheet sheet, int row) {
		int col = 1;
		header(sheet, row, col++, "Process UUID");
		header(sheet, row, col++, "Process");
		header(sheet, row, col++, "Location");
	}

	/** Writes the process information into the given column starting at row 1. */
	void writeProcessColInfo(Sheet sheet, int col, Process process) {
		int row = 1;
		Excel.cell(sheet, row++, col, process.getId());
		Excel.cell(sheet, row++, col, process.getName());
		if (process.getLocation() != null)
			Excel.cell(sheet, row, col, process.getLocation().getCode());
	}

	/** Writes the process information into the given row starting at column 1. */
	void writeProcessRowInfo(Sheet sheet, int row, Process process) {
		int col = 1;
		Excel.cell(sheet, row, col++, process.getId());
		Excel.cell(sheet, row, col++, process.getName());
		if (process.getLocation() != null)
			Excel.cell(sheet, row, col++, process.getLocation().getCode());
	}

	/**
	 * Writes the impact category header into the given column starting at row
	 * 1. The next free row is 1 + IMPACT_INFO_SIZE.
	 */
	void writeImpactColHeader(Sheet sheet, int col) {
		int row = 1;
		header(sheet, row++, col, "Impact category UUID");
		header(sheet, row++, col, "Impact category");
		header(sheet, row++, col, "Reference unit");
	}

	/**
	 * Writes the impact category information into the given column starting at
	 * row 1.
	 */
	void writeImpactColInfo(Sheet sheet, int col,
			ImpactCategoryDescriptor impact) {
		int row = 1;
		Excel.cell(sheet, row++, col, impact.getId());
		Excel.cell(sheet, row++, col, impact.getName());
		Excel.cell(sheet, row++, col, impact.getReferenceUnit());
	}

	/**
	 * Writes the impact category header into the given row starting at column
	 * 1. The next free column is 1 + IMPACT_INFO_SIZE.
	 */
	void writeImpactRowHeader(Sheet sheet, int row) {
		int col = 1;
		header(sheet, row, col++, "Impact category UUID");
		header(sheet, row, col++, "Impact category");
		header(sheet, row, col++, "Reference unit");
	}

	/**
	 * Writes the impact category information into the given row starting at
	 * column 1.
	 */
	void writeImpactRowInfo(Sheet sheet, int row,
			ImpactCategoryDescriptor impact) {
		int col = 1;
		Excel.cell(sheet, row, col++, impact.getId());
		Excel.cell(sheet, row, col++, impact.getName());
		Excel.cell(sheet, row, col++, impact.getReferenceUnit());
	}

	/**
	 * Writes the flow-information header into the given row starting at column
	 * 1. The next free column is 1 + FLOW_INFO_SIZE.
	 */
	void writeFlowRowHeader(Sheet sheet, int row) {
		int col = 1;
		header(sheet, row, col++, "Flow UUID");
		header(sheet, row, col++, "Flow");
		header(sheet, row, col++, "Category");
		header(sheet, row, col++, "Sub-category");
		header(sheet, row, col++, "Unit");
	}

	/**
	 * Writes the given flow information into the given row starting at column
	 * 1. The next free column is 1 + FLOW_INFO_SIZE.
	 */
	void writeFlowRowInfo(Sheet sheet, int row, FlowInfo info) {
		int col = 1;
		Excel.cell(sheet, row, col++, info.getId());
		Excel.cell(sheet, row, col++, info.getName());
		Excel.cell(sheet, row, col++, info.getCategory());
		Excel.cell(sheet, row, col++, info.getSubCategory());
		Excel.cell(sheet, row, col++, info.getUnit());
	}

	/** Makes a header entry in the given row and column. */
	void header(Sheet sheet, int row, int col, String val) {
		Excel.cell(sheet, row, col, val).setCellStyle(headerStyle);
	}

	/** Visitor for the flows in the analysis result. */
	interface FlowVisitor {
		void next(Flow flow, FlowInfo info, boolean input);
	}

}
