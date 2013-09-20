package org.openlca.app.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.AnalysisFlowResult;
import org.openlca.core.results.AnalysisResult;

class FlowContributionProvider implements
		IProcessContributionProvider<FlowDescriptor> {

	private AnalysisResult result;
	private EntityCache cache;

	public FlowContributionProvider(AnalysisResult result) {
		this.result = result;
		this.cache = Database.getEntityCache();
	}

	@Override
	public FlowDescriptor[] getElements() {
		Set<FlowDescriptor> set = result.getFlowResults().getFlows(cache);
		return set.toArray(new FlowDescriptor[set.size()]);
	}

	@Override
	public AnalysisResult getAnalysisResult() {
		return result;
	}

	@Override
	public List<ProcessContributionItem> getItems(FlowDescriptor selection,
			double cutOff) {
		return getContributions(selection, cutOff, true);
	}

	@Override
	public List<ProcessContributionItem> getHotSpots(FlowDescriptor selection,
			double cutOff) {
		return getContributions(selection, cutOff, false);
	}

	private List<ProcessContributionItem> getContributions(FlowDescriptor flow,
			double cutOff, boolean total) {
		if (result == null || flow == null)
			return Collections.emptyList();
		List<AnalysisFlowResult> flowResults = result.getFlowResults()
				.getForFlow(flow, Database.getEntityCache());
		if (flowResults.isEmpty())
			return Collections.emptyList();
		double refVale = getRefValue(flowResults);
		String unit = Labels.getRefUnit(flow, cache);
		List<ProcessContributionItem> items = new ArrayList<>();
		for (AnalysisFlowResult result : flowResults) {
			double val = total ? result.getTotalResult() : result
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
		item.setTotalAmount(result.getTotalResult());
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
		double max = first.getTotalResult();
		double min = max;
		for (int i = 1; i < flowResults.size(); i++) {
			AnalysisFlowResult next = flowResults.get(i);
			double nextVal = next.getTotalResult();
			max = Math.max(max, nextVal);
			min = Math.min(min, nextVal);
		}
		return Math.max(Math.abs(max), Math.abs(min));
	}

}
