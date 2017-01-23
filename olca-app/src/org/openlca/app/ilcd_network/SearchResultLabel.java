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
		if (element instanceof ProcessDescriptor) {
			ProcessDescriptor process = (ProcessDescriptor) element;
			text = createLabel(process, columnIndex);
		}
		return text;
	}

	private String createLabel(ProcessDescriptor process, int columnIndex) {
		switch (columnIndex) {
		case SearchResultViewer.NAME_COLUMN:
			return LangString.getFirst(process.name, "en");
		case SearchResultViewer.LOCATION_COLUMN:
			return process.location;
		case SearchResultViewer.TIME_COLUMN:
			return createTimeLabel(process);
		case SearchResultViewer.TYPE_COLUMN:
			return createTypeLabel(process);
		default:
			return null;
		}
	}

	private String createTimeLabel(ProcessDescriptor process) {
		String timeSpan = "";
		Time time = process.time;
		if (time != null) {
			String startYear = yearToString(time.referenceYear);
			String endYear = yearToString(time.validUntil);
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
		if (process == null || process.type == null)
			return null;
		return process.type.value();
	}

}
