package org.openlca.app.editors.reports.model;

public class ReportSection {

	public int index;
	public String title;
	public String text;
	public String componentId;

	@Override
	protected ReportSection clone() {
		ReportSection clone = new ReportSection();
		clone.index = index;
		clone.text = text;
		clone.title = title;
		return clone;
	}
}
