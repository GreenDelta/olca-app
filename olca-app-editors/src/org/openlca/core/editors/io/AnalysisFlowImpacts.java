package org.openlca.core.editors.io;

import org.apache.poi.ss.usermodel.Sheet;
import org.openlca.core.editors.io.AnalysisResultExport.FlowVisitor;
import org.openlca.core.editors.model.FlowInfo;
import org.openlca.core.model.Flow;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.core.model.results.Contribution;
import org.openlca.core.model.results.ContributionSet;
import org.openlca.core.model.results.FlowImpactContribution;

/**
 * Writes the contributions of the flows to the overall impact assessment result
 * into an Excel sheet.
 */
class AnalysisFlowImpacts {

	private Sheet sheet;
	private AnalysisResult result;
	private AnalysisResultExport export;

	private int startRow;
	private int startCol;

	private AnalysisFlowImpacts(Sheet sheet, AnalysisResult result,
			AnalysisResultExport export) {
		this.sheet = sheet;
		this.result = result;
		this.export = export;
		startRow = export.IMPACT_INFO_SIZE + 1;
		startCol = export.FLOW_INFO_SIZE;
	}

	public static void write(Sheet sheet, AnalysisResult result,
			AnalysisResultExport export) {
		new AnalysisFlowImpacts(sheet, result, export).doIt();
	}

	private void doIt() {
		export.writeImpactColHeader(sheet, startCol);
		export.writeFlowRowHeader(sheet, startRow);
		export.visitFlows(new FlowInfoWriter());
		FlowImpactContribution contribution = new FlowImpactContribution(result);
		FlowValueWriter valueWriter = new FlowValueWriter();
		int col = startCol + 1;
		for (ImpactCategoryDescriptor impact : export.getImpacts()) {
			export.writeImpactColInfo(sheet, col, impact);
			ContributionSet<Flow> contributions = contribution
					.calculate(impact);
			valueWriter.setNext(contributions, col);
			export.visitFlows(valueWriter);
			col++;
		}
		// Excel.autoSize(sheet, 1, 2, 3, 4, 5, 6);
	}

	private class FlowInfoWriter implements FlowVisitor {

		private int row = startRow + 1;

		@Override
		public void next(Flow flow, FlowInfo info, boolean input) {
			export.writeFlowRowInfo(sheet, row++, info);
		}
	}

	private class FlowValueWriter implements FlowVisitor {

		private int row;
		private int col;
		private ContributionSet<Flow> contributions;

		private void setNext(ContributionSet<Flow> contributions, int col) {
			this.contributions = contributions;
			row = startRow + 1;
			this.col = col;
		}

		public void next(Flow flow, FlowInfo info, boolean input) {
			Contribution<Flow> contribution = contributions
					.getContribution(flow);
			if (contribution == null)
				return;
			Excel.cell(sheet, row++, col, contribution.getAmount());
		}
	}

}
