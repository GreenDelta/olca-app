package org.openlca.app.results.grouping;

import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ProcessGrouping;

class GroupResultTable {

	private final int GROUP_COL = 0;
	private final int AMOUNT_COL = 1;
	private final int UNIT_COL = 2;

	private final String GROUP = M.Group;
	private final String AMOUNT = M.Amount;
	private final String UNIT = M.Unit;

	private TableViewer viewer;
	private String unit;

	public GroupResultTable(Composite parent) {
		String[] colLabels = { GROUP, AMOUNT, UNIT };
		viewer = Tables.createViewer(parent, colLabels, new GroupResultLabel());
		Tables.bindColumnWidths(viewer.getTable(), 0.5, 0.25, 0.25);
		UI.gridData(viewer.getControl(), true, false).heightHint = 200;
		Actions.bind(viewer, TableClipboard.onCopySelected(viewer));
		viewer.getTable().getColumns()[1].setAlignment(SWT.RIGHT);
	}

	public void setInput(List<Contribution<ProcessGrouping>> items,
			String unit) {
		this.unit = unit;
		viewer.setInput(items);
	}

	private class GroupResultLabel extends ColumnLabelProvider implements
			ITableLabelProvider {

		private ContributionImage image = new ContributionImage();

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
			Contribution<?> item = (Contribution) element;
			return image.get(item.share);
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int column) {
			if (!(element instanceof ContributionItem))
				return null;
			Contribution<ProcessGrouping> resultItem = (Contribution<ProcessGrouping>) element;
			switch (column) {
			case GROUP_COL:
				return getName(resultItem);
			case AMOUNT_COL:
				return Double.toString(resultItem.amount);
			case UNIT_COL:
				return unit;
			default:
				return null;
			}
		}

		private String getName(Contribution<ProcessGrouping> resultItem) {
			ProcessGrouping group = resultItem.item;
			if (group != null)
				return group.name;
			return null;
		}
	}

}
