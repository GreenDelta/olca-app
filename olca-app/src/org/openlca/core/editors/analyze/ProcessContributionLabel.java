package org.openlca.core.editors.analyze;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.util.Numbers;
import org.openlca.core.editors.ContributionImage;

/** Label and image provider for the process contribution viewer. */
class ProcessContributionLabel extends ColumnLabelProvider implements
		ITableLabelProvider {

	private ContributionImage contributionImage = new ContributionImage(
			Display.getCurrent());

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (!(element instanceof ProcessContributionItem))
			return null;
		if (columnIndex != ProcessContributionViewer.CONTRIBUTION)
			return null;
		ProcessContributionItem item = (ProcessContributionItem) element;
		return contributionImage.getForTable(item.getContribution());
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof ProcessContributionItem))
			return null;
		ProcessContributionItem item = (ProcessContributionItem) element;
		switch (columnIndex) {
		case ProcessContributionViewer.CONTRIBUTION:
			return Numbers.percent(item.getContribution());
		case ProcessContributionViewer.NAME:
			return item.getProcessName();
		case ProcessContributionViewer.SINGLE_AMOUNT:
			return Numbers.format(item.getSingleAmount());
		case ProcessContributionViewer.TOTAL_AMOUNT:
			return Numbers.format(item.getTotalAmount());
		case ProcessContributionViewer.UNIT:
			return item.getUnit();
		default:
			return null;
		}
	}

	@Override
	public void dispose() {
		contributionImage.dispose();
		super.dispose();
	}

}
