package org.openlca.app.ilcd_network;

import java.math.BigInteger;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.ilcd.descriptors.ProcessDescriptor;
import org.openlca.ilcd.descriptors.Time;

/**
 * The label provider for a search result row.
 * 
 * @author Michael Srocka
 * 
 */
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
			return process.getName() != null ? process.getName().getValue()
					: "";
		case SearchResultViewer.LOCATION_COLUMN:
			return process.getLocation();
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
		Time time = process.getTime();
		if (time != null) {
			String startYear = yearToString(time.getReferenceYear());
			String endYear = yearToString(time.getValidUntil());
			timeSpan = startYear + " - " + endYear;
		}
		return timeSpan;
	}

	private String yearToString(BigInteger year) {
		if (year == null)
			return "?";
		return year.toString();
	}

	private String createTypeLabel(ProcessDescriptor process) {
		if (process == null || process.getType() == null)
			return null;
		return process.getType().value();
	}

}
