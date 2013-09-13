package org.openlca.app.analysis;

import java.io.File;
import java.util.List;


class ContributionExportData {

	private File file;
	private String title;
	private String itemName;
	private String selectedItem;
	private double cutoff;
	private List<ProcessContributionItem> items;
	private String orderType;

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public double getCutoff() {
		return cutoff;
	}

	public void setCutoff(double cutoff) {
		this.cutoff = cutoff;
	}

	public List<ProcessContributionItem> getItems() {
		return items;
	}

	public void setItems(List<ProcessContributionItem> items) {
		this.items = items;
	}

	public String getSelectedItem() {
		return selectedItem;
	}

	public void setSelectedItem(String selectedItem) {
		this.selectedItem = selectedItem;
	}

	public String getOrderType() {
		return orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

}
