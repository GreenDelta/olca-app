package org.openlca.app.ilcd_network;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.ilcd.commons.LangString;
import org.openlca.ilcd.descriptors.ProcessDescriptor;
import org.openlca.ilcd.descriptors.Time;

class SearchResultLabel extends LabelProvider implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		String text = null;
		if (element instanceof ProcessDescriptor process) {
			text = createLabel(process, columnIndex);
		}
		return text;
	}

	private String createLabel(ProcessDescriptor process, int col) {
		return switch (col) {
			case SearchResultViewer.NAME_COLUMN ->
					LangString.getFirst(process.getName(), "en");
			case SearchResultViewer.LOCATION_COLUMN -> process.getLocation();
			case SearchResultViewer.TIME_COLUMN -> createTimeLabel(process);
			case SearchResultViewer.TYPE_COLUMN -> createTypeLabel(process);
			default -> null;
		};
	}

	private String createTimeLabel(ProcessDescriptor process) {
		String timeSpan = "";
		Time time = process.getTime();
		if (time != null) {
			String startYear = yearToString(time.getReferenceYear());
			String endYear = yearToString(time.getValidUntil());
			timeSpan = startYear + " - " + endYear;
		}
		return timeSpan;
	}

	private String yearToString(Integer year) {
		if (year == null)
			return "?";
		return year.toString();
	}

	private String createTypeLabel(ProcessDescriptor process) {
		if (process == null || process.getProcessType() == null)
			return null;
		return process.getProcessType().value();
	}

}
