package org.openlca.app.results.viz;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

class BubbleChartDataSet {

	private String refName;
	private String refUnit;
	private double totalAmount;
	private List<Item> items = new ArrayList<>();

	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	public String getRefName() {
		return refName;
	}

	public void setRefName(String refName) {
		this.refName = refName;
	}

	public String getRefUnit() {
		return refUnit;
	}

	public void setRefUnit(String refUnit) {
		this.refUnit = refUnit;
	}

	public double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public List<Item> getItems() {
		return items;
	}

	static class Item {

		private String name;
		private double amount;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public double getAmount() {
			return amount;
		}

		public void setAmount(double amount) {
			this.amount = amount;
		}
	}

}
