package org.openlca.app.results;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.openlca.app.Messages;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.openlca.core.results.ContributionItem;

/**
 * Table for showing the result contributions for locations of an analysis
 * result.
 */
class LocationContributionTable {

	private final int LOCATION_COL = 0;
	private final int AMOUNT_COL = 1;
	private final int UNIT_COL = 2;
	private String[] COLUMN_LABELS = { Messages.Location, Messages.Amount,
			Messages.Unit };

	private TableViewer viewer;
	private String unit;

	public LocationContributionTable(Composite parent) {
		UI.gridLayout(parent, 1);
		viewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION
				| SWT.VIRTUAL | SWT.MULTI);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new LabelProvider());
		Table table = viewer.getTable();
		UI.gridData(table, true, true).heightHint = 150;
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		for (String col : COLUMN_LABELS) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(col);
		}
		Tables.bindColumnWidths(table, 0.5, 0.25, 0.25);
		Actions.bind(viewer, TableClipboard.onCopy(viewer));
	}

	public void setInput(List<ContributionItem<Location>> contributions,
			String unit) {
		this.unit = unit;
		viewer.setInput(contributions);
	}

	private class LabelProvider extends ColumnLabelProvider implements
			ITableLabelProvider {

		private ContributionImage image = new ContributionImage(
				Display.getCurrent());

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Image getColumnImage(Object element, int col) {
			if (col != 0)
				return null;
			if (!(element instanceof ContributionItem))
				return null;
			ContributionItem<Location> contribution = ContributionItem.class
					.cast(element);
			return image.getForTable(contribution.getShare());
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int col) {
			if (!(element instanceof ContributionItem))
				return null;
			ContributionItem<Location> contribution = ContributionItem.class
					.cast(element);
			switch (col) {
			case LOCATION_COL:
				return contribution.getItem() == null ? Messages.Other
						: contribution.getItem().getName();
			case AMOUNT_COL:
				return Numbers.format(contribution.getAmount());
			case UNIT_COL:
				return unit;
			default:
				return null;
			}
		}

	}

}
