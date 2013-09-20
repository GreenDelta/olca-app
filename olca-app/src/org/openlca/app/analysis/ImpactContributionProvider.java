package org.openlca.app.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openlca.app.db.Database;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.AnalysisImpactResult;
import org.openlca.core.results.AnalysisResult;

class ImpactContributionProvider implements
		IProcessContributionProvider<ImpactCategoryDescriptor> {

	private AnalysisResult result;
	private EntityCache cache = Database.getEntityCache();

	public ImpactContributionProvider(AnalysisResult result) {
		this.result = result;
	}

	@Override
	public ImpactCategoryDescriptor[] getElements() {
		if (!result.hasImpactResults())
			return new ImpactCategoryDescriptor[0];
		Set<ImpactCategoryDescriptor> set = result.getImpactResults()
				.getImpacts(cache);
		return set.toArray(new ImpactCategoryDescriptor[set.size()]);
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
		List<AnalysisImpactResult> results = result.getImpactResults()
				.getForImpact(selection, cache);
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
		if (total)
			return r.getTotalResult();
		else
			return r.getSingleResult();
	}

}
