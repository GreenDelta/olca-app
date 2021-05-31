package org.openlca.app.editors.projects.reports.model;

import org.openlca.core.model.Copyable;

public class ReportSection implements Copyable<ReportSection> {

	public int index;
	public String title;
	public String text;
	public String componentId;

	@Override
	public ReportSection copy() {
		var copy = new ReportSection();
		copy.index = index;
		copy.text = text;
		copy.title = title;
		copy.componentId = componentId;
		return copy;
	}
}
