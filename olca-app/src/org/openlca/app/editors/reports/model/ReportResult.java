package org.openlca.app.editors.reports.model;

import java.util.ArrayList;
import java.util.List;

class ReportResult {

	private final int indicatorId;
	private List<VariantResult> variantResults = new ArrayList<>();

	public ReportResult(int indicatorId) {
		this.indicatorId = indicatorId;
	}

	public int getIndicatorId() {
		return indicatorId;
	}

	public List<VariantResult> getVariantResults() {
		return variantResults;
	}

	static class VariantResult {

		private String variant;
		private double totalAmount;
		private List<Contribution> contributions = new ArrayList<>();

		public String getVariant() {
			return variant;
		}

		public void setVariant(String variant) {
			this.variant = variant;
		}

		public double getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(double totalAmount) {
			this.totalAmount = totalAmount;
		}

		public List<Contribution> getContributions() {
			return contributions;
		}
	}

	static class Contribution {

		private long processId;
		private double amount;
		private boolean rest;

		public long getProcessId() {
			return processId;
		}

		public void setProcessId(long processId) {
			this.processId = processId;
		}

		public double getAmount() {
			return amount;
		}

		public void setAmount(double amount) {
			this.amount = amount;
		}

		public boolean isRest() {
			return rest;
		}

		public void setRest(boolean rest) {
			this.rest = rest;
		}
	}
}
