package org.openlca.app.editors.projects.reports.model;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.results.Contribution;

class ReportIndicatorResult {

	final int indicatorId;
	final List<VariantResult> variantResults = new ArrayList<>();

	public ReportIndicatorResult(int indicatorId) {
		this.indicatorId = indicatorId;
	}

	static class VariantResult {
		String variant;
		double totalAmount;
		final List<Contribution<Long>> contributions = new ArrayList<>();
	}

}
