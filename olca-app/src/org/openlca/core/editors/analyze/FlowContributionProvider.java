package org.openlca.core.editors.analyze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.results.AnalysisFlowResult;
import org.openlca.core.results.AnalysisResult;

class FlowContributionProvider implements IProcessContributionProvider<Flow> {

	private AnalysisResult result;
	private IDatabase database;

	public FlowContributionProvider(IDatabase database, AnalysisResult result) {
		this.result = result;
		this.database = database;
	}

	@Override
	public IDatabase getDatabase() {
		return database;
	}

	@Override
	public AnalysisResult getAnalysisResult() {
		return result;
	}

	@Override
	public List<ProcessContributionItem> getItems(Flow selection, double cutOff) {
		return getContributions(selection, cutOff, true);
	}

	@Override
	public List<ProcessContributionItem> getHotSpots(Flow selection,
			double cutOff) {
		return getContributions(selection, cutOff, false);
	}

	private List<ProcessContributionItem> getContributions(Flow selection,
			double cutOff, boolean total) {
		if (result == null || selection == null)
			return Collections.emptyList();
		List<AnalysisFlowResult> flowResults = result.getFlowResults(selection);
		if (flowResults.isEmpty())
			return Collections.emptyList();
		double refVale = getRefValue(flowResults);
		String unit = flowUnit(selection);
		List<ProcessContributionItem> items = new ArrayList<>();
		for (AnalysisFlowResult result : flowResults) {
			double val = total ? result.getAggregatedResult() : result
					.getSingleResult();
			double c = contribution(val, refVale);
			if (cutOff == 0 || Math.abs(c) >= cutOff)
				items.add(makeItem(result, c, unit));
		}
		return items;
	}

	private ProcessContributionItem makeItem(AnalysisFlowResult result,
			double c, String unit) {
		ProcessContributionItem item = new ProcessContributionItem();
		item.setContribution(c);
		if (result.getProcess() != null)
			item.setProcessName(result.getProcess().getName());
		item.setSingleAmount(result.getSingleResult());
		item.setTotalAmount(result.getAggregatedResult());
		item.setUnit(unit);
		return item;
	}

	private double contribution(double val, double refValue) {
		if (refValue == 0)
			return 0;
		return (val / refValue);
	}

	private double getRefValue(List<AnalysisFlowResult> flowResults) {
		AnalysisFlowResult first = flowResults.get(0);
		double max = first.getAggregatedResult();
		double min = max;
		for (int i = 1; i < flowResults.size(); i++) {
			AnalysisFlowResult next = flowResults.get(i);
			double nextVal = next.getAggregatedResult();
			max = Math.max(max, nextVal);
			min = Math.min(min, nextVal);
		}
		return Math.max(Math.abs(max), Math.abs(min));
	}

	private String flowUnit(Flow flow) {
		if (flow == null)
			return null;
		FlowProperty refProp = flow.getReferenceFlowProperty();
		if (refProp == null)
			return null;
		UnitGroup unitGroup = refProp.getUnitGroup();
		if (unitGroup == null)
			return null;
		Unit unit = unitGroup.getReferenceUnit();
		if (unit == null)
			return null;
		return unit.getName();
	}
}
