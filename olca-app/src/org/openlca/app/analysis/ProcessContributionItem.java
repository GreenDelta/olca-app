package org.openlca.app.analysis;

class ProcessContributionItem implements Comparable<ProcessContributionItem> {

	private double contribution;
	private String processName;
	private double totalAmount;
	private double singleAmount;
	private String unit;

	public double getContribution() {
		return contribution;
	}

	public void setContribution(double contribution) {
		this.contribution = contribution;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}

	public double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public double getSingleAmount() {
		return singleAmount;
	}

	public void setSingleAmount(double singleAmount) {
		this.singleAmount = singleAmount;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	@Override
	public int compareTo(ProcessContributionItem other) {
		if (other == null)
			return 1;
		return Double.compare(other.getContribution(), this.contribution);
	}

}
