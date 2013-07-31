package org.openlca.core.editors.analyze;

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
import org.openlca.app.util.UI;
import org.openlca.core.editors.ContributionImage;
import org.openlca.core.model.results.Contribution;
import org.openlca.core.model.results.ProcessGrouping;

class GroupResultTable {

	private final int GROUP_COL = 0;
	private final int AMOUNT_COL = 1;

	private final String GROUP = Messages.Common_Group;
	private final String AMOUNT = Messages.Common_Amount;
	private final String UNIT = Messages.Common_Unit;

	private TableViewer viewer;

	public GroupResultTable(Composite parent) {
		viewer = new TableViewer(parent);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new GroupResultLabel());
		viewer.setSorter(new ContributionSorter());
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		String[] colLabels = { GROUP, AMOUNT, UNIT };
		for (String col : colLabels) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(col);
		}
		UI.bindColumnWidths(table, 0.5, 0.25, 0.25);
	}

	public TableViewer getViewer() {
		return viewer;
	}

	private class GroupResultLabel extends ColumnLabelProvider implements
			ITableLabelProvider {

		private ContributionImage image = new ContributionImage(
				Display.getCurrent());

		@Override
		public void dispose() {
			super.dispose();
			image.dispose();
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Image getColumnImage(Object element, int column) {
			if (!(element instanceof Contribution) || column != 0)
				return null;
			Contribution resultItem = (Contribution) element;
			return image.getForTable(resultItem.getShare());
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int column) {
			if (!(element instanceof Contribution))
				return null;
			Contribution<ProcessGrouping> resultItem = (Contribution<ProcessGrouping>) element;
			switch (column) {
			case GROUP_COL:
				return getName(resultItem);
			case AMOUNT_COL:
				return Double.toString(resultItem.getAmount());
			default:
				return null;
			}
		}

		private String getName(Contribution<ProcessGrouping> resultItem) {
			ProcessGrouping group = resultItem.getItem();
			if (group != null)
				return group.getName();
			return null;
		}
	}

}
