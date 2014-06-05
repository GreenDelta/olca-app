package org.openlca.app.editors.reports.model;

public class ReportSection {

	private int index;
	private String title;
	private String text;
	private String componentId;

	@Override
	protected ReportSection clone() {
		ReportSection clone = new ReportSection();
		clone.setIndex(getIndex());
		clone.setText(getText());
		clone.setTitle(getTitle());
		return clone;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}
}
