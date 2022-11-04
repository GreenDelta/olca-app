package org.openlca.app.results.grouping;

import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ProcessGrouping;

class GroupResultTable {

	private final TableViewer viewer;
	private String unit;

	public GroupResultTable(Composite parent) {
		String[] colLabels = {M.Group, M.Amount, M.Unit};
		viewer = Tables.createViewer(parent, colLabels, new GroupResultLabel());
		Tables.bindColumnWidths(viewer.getTable(), 0.5, 0.25, 0.25);
		UI.gridData(viewer.getControl(), true, false).heightHint = 200;
		Actions.bind(viewer, TableClipboard.onCopySelected(viewer));
		viewer.getTable().getColumns()[1].setAlignment(SWT.RIGHT);
	}

	public void setInput(List<Contribution<ProcessGrouping>> items, String unit) {
		this.unit = unit;
		viewer.setInput(items);
	}

	private class GroupResultLabel extends ColumnLabelProvider
			implements ITableLabelProvider {

		private final ContributionImage image = new ContributionImage();

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int column) {
			return column == 0 && obj instanceof Contribution<?> item
					? image.get(item.share)
					: null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int column) {
			if (!(element instanceof Contribution<?> con))
				return null;
			var c = (Contribution<ProcessGrouping>) con;
			return switch (column) {
				case 0 -> con.item != null ? c.item.name : null;
				case 1 -> Numbers.format(c.amount);
				case 2 -> unit;
				default -> null;
			};
		}
	}
}
