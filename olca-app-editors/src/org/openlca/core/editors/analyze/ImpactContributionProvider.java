package org.openlca.core.editors.analyze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisImpactResult;
import org.openlca.core.model.results.AnalysisResult;

class ImpactContributionProvider implements
		IProcessContributionProvider<ImpactCategoryDescriptor> {

	private AnalysisResult result;

	public ImpactContributionProvider(AnalysisResult result) {
		this.result = result;
	}

	@Override
	public IDatabase getDatabase() {
		return null;
	}

	@Override
	public AnalysisResult getAnalysisResult() {
		return result;
	}

	@Override
	public List<ProcessContributionItem> getItems(
			ImpactCategoryDescriptor selection, double cutOff) {
		return getItems(selection, cutOff, true);
	}

	@Override
	public List<ProcessContributionItem> getHotSpots(
			ImpactCategoryDescriptor selection, double cutOff) {
		return getItems(selection, cutOff, false);
	}

	private List<ProcessContributionItem> getItems(
			ImpactCategoryDescriptor selection, double cutOff, boolean total) {
		List<AnalysisImpactResult> results = result.getImpactResults(selection);
		if (results.isEmpty())
			return Collections.emptyList();
		double refValue = getRefValue(results);
		List<ProcessContributionItem> items = new ArrayList<>();
		String unit = selection.getReferenceUnit();
		for (AnalysisImpactResult r : results) {
			double c = contribution(r, refValue, total);
			if (cutOff == 0 || Math.abs(c) >= cutOff) {
				items.add(makeItem(r, c, unit));
			}
		}
		return items;
	}

	private ProcessContributionItem makeItem(AnalysisImpactResult r, double c,
			String unit) {
		ProcessContributionItem item = new ProcessContributionItem();
		item.setContribution(c);
		if (r.getProcess() != null)
			item.setProcessName(r.getProcess().getName());
		item.setSingleAmount(getValue(r, false));
		item.setTotalAmount(getValue(r, true));
		item.setUnit(unit);
		return item;
	}

	private double contribution(AnalysisImpactResult r, double refValue,
			boolean total) {
		double val = getValue(r, total);
		if (refValue == 0)
			return 0;
		return (val / refValue);
	}

	private double getRefValue(List<AnalysisImpactResult> results) {
		double max = getValue(results.get(0), true);
		double min = max;
		for (int i = 1; i < results.size(); i++) {
			double val = getValue(results.get(i), true);
			max = Math.max(max, val);
			min = Math.min(min, val);
		}
		return Math.max(Math.abs(max), Math.abs(min));
	}

	private double getValue(AnalysisImpactResult r, boolean total) {
		if (r == null)
			return 0;
		if (total) {
			if (r.getAggregatedResult() == null)
				return 0;
			return r.getAggregatedResult().getValue();
		}
		if (r.getSingleResult() == null)
			return 0;
		return r.getSingleResult().getValue();
	}

}
