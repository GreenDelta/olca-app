package org.openlca.core.editors.io;

import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.openlca.core.database.CategoryPath;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.SimulationResult;
import org.openlca.core.math.Statistics;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimulationExportFlow implements Comparable<SimulationExportFlow> {

	private Flow flow;
	private boolean input;
	private String name;
	private String category;
	private String unit;
	private String property;

	public SimulationExportFlow(Flow flow, boolean input, IDatabase database) {
		this.flow = flow;
		this.input = input;
		this.name = flow.getName();
		this.category = CategoryPath.getShort(flow.getCategoryId(), database);
		loadFlowInfo(flow, database);
	}

	public boolean isInput() {
		return input;
	}

	private void loadFlowInfo(Flow flow, IDatabase database) {
		try {
			FlowDao dao = new FlowDao(database.getEntityFactory());
			unit = dao.getRefUnitName(flow);
			FlowProperty prop = flow.getReferenceFlowProperty();
			property = prop != null ? prop.getName() : "n.a.";
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Could not load flow information", e);
		}
	}

	@Override
	public int compareTo(SimulationExportFlow other) {
		int c = Strings.compare(name, other.name);
		if (c != 0)
			return c;
		return Strings.compare(category, other.category);
	}

	public void writeRow(HSSFRow aRow, SimulationResult result) {
		Excel.cell(aRow, 0, name);
		Excel.cell(aRow, 1, category);
		Excel.cell(aRow, 2, property);
		Excel.cell(aRow, 3, unit);
		List<Double> results = result.getResults(flow);
		Statistics stat = new Statistics(results, 100);
		Excel.cell(aRow, 4, stat.getMean());
		Excel.cell(aRow, 5, stat.getStandardDeviation());
		Excel.cell(aRow, 6, stat.getMinimum());
		Excel.cell(aRow, 7, stat.getMaximum());
		Excel.cell(aRow, 8, stat.getMedian());
		Excel.cell(aRow, 9, stat.getPercentileValue(5));
		Excel.cell(aRow, 10, stat.getPercentileValue(95));
	}

	public static String[] getHeaders() {
		return new String[] { "Flow", "Category", "Property", "Unit", "Mean",
				"Standard deviation", "Minimum", "Maximum", "Median",
				"5% Percentile", "95% Percentile" };
	}
}
