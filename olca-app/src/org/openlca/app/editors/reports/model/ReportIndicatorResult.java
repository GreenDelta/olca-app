package org.openlca.app.editors.reports.model;

import java.util.ArrayList;
import java.util.List;

class ReportIndicatorResult {

	final int indicatorId;
	final List<VariantResult> variantResults = new ArrayList<>();

	public ReportIndicatorResult(int indicatorId) {
		this.indicatorId = indicatorId;
	}

	static class VariantResult {
		String variant;
		double totalAmount;
		final List<Contribution> contributions = new ArrayList<>();
	}

	static class Contribution {
		long processId;
		double amount;
		boolean rest;
	}
}
