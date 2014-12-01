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
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ProcessGrouping;

class GroupResultTable {

	private final int GROUP_COL = 0;
	private final int AMOUNT_COL = 1;
	private final int UNIT_COL = 2;

	private final String GROUP = Messages.Group;
	private final String AMOUNT = Messages.Amount;
	private final String UNIT = Messages.Unit;

	private TableViewer viewer;
	private String unit;

	public GroupResultTable(Composite parent) {
		viewer = new TableViewer(parent);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new GroupResultLabel());
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		String[] colLabels = { GROUP, AMOUNT, UNIT };
		for (String col : colLabels) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(col);
		}
		Tables.bindColumnWidths(table, 0.5, 0.25, 0.25);
		UI.gridData(viewer.getControl(), true, false).heightHint = 200;
		Actions.bind(viewer, TableClipboard.onCopy(viewer));
	}

	public void setInput(List<ContributionItem<ProcessGrouping>> items,
			String unit) {
		this.unit = unit;
		viewer.setInput(items);
	}

	private class GroupResultLabel extends ColumnLabelProvider implements
			ITableLabelProvider {

		private ContributionImage image = new ContributionImage(
				Display.getCurrent());

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Image getColumnImage(Object element, int column) {
			if (!(element instanceof ContributionItem) || column != 0)
				return null;
			ContributionItem<?> item = (ContributionItem) element;
			return image.getForTable(item.getShare());
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int column) {
			if (!(element instanceof ContributionItem))
				return null;
			ContributionItem<ProcessGrouping> resultItem = (ContributionItem<ProcessGrouping>) element;
			switch (column) {
			case GROUP_COL:
				return getName(resultItem);
			case AMOUNT_COL:
				return Double.toString(resultItem.getAmount());
			case UNIT_COL:
				return unit;
			default:
				return null;
			}
		}

		private String getName(ContributionItem<ProcessGrouping> resultItem) {
			ProcessGrouping group = resultItem.getItem();
			if (group != null)
				return group.getName();
			return null;
		}
	}

}
