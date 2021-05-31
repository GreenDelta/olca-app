package org.openlca.app.editors.projects.reports.model;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.results.Contribution;

public class ReportIndicatorResult {

	final int indicatorId;
	public final List<VariantResult> variantResults = new ArrayList<>();

	public ReportIndicatorResult(int indicatorId) {
		this.indicatorId = indicatorId;
	}

	public static class VariantResult {
		String variant;
		double totalAmount;
		public final List<Contribution<Long>> contributions = new ArrayList<>();
	}

}
