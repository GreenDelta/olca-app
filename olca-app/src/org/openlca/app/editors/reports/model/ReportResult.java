package org.openlca.app.editors.reports.model;

import java.util.ArrayList;
import java.util.List;

class ReportResult {

	private String indicator;
	private String unit;
	private List<VariantResult> variantResults = new ArrayList<>();

	public String getIndicator() {
		return indicator;
	}

	public void setIndicator(String indicator) {
		this.indicator = indicator;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
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

		private String process;
		private double amount;
		private boolean rest;

		public String getProcess() {
			return process;
		}

		public void setProcess(String process) {
			this.process = process;
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
