package org.openlca.app.results.contributions;

import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.openlca.app.Messages;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.results.contributions.LocationContributionPage.TreeItem;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.TreeClipboard;
import org.openlca.app.util.trees.Trees;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;

/**
 * Table for showing the result contributions for locations of an analysis
 * result.
 */
class LocationContributionTree {

	private final int LOCATION_COL = 0;
	private final int PROCESS_COL = 1;
	private final int AMOUNT_COL = 2;
	private final int UNIT_COL = 3;

	private String[] COLUMN_LABELS = { Messages.Location, Messages.Process,
			Messages.Amount, Messages.Unit };

	private TreeViewer viewer;
	private String unit;

	public LocationContributionTree(Composite parent, boolean withMinHeight) {
		UI.gridLayout(parent, 1);
		viewer = new TreeViewer(parent);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		Tree tree = viewer.getTree();
		GridData gridData = UI.gridData(tree, true, true);
		if (withMinHeight)
			gridData.minimumHeight = 150;
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		for (String col : COLUMN_LABELS) {
			TreeColumn column = new TreeColumn(tree, SWT.NONE);
			column.setText(col);
		}
		Trees.bindColumnWidths(tree, 0.35, 0.35, 0.15, 0.15);
		Actions.bind(viewer, TreeClipboard.onCopy(viewer));
	}

	public void setInput(List<TreeItem> contributions, String unit) {
		this.unit = unit;
		viewer.setInput(contributions);
	}

	private class ContentProvider implements ITreeContentProvider {

		@Override
		@SuppressWarnings("unchecked")
		public Object[] getElements(Object obj) {
			if (obj == null)
				return new Object[0];
			List<TreeItem> items = List.class.cast(obj);
			return items.toArray(new TreeItem[items.size()]);
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (!(parent instanceof TreeItem))
				return new Object[0];
			TreeItem e = (TreeItem) parent;
			List<ContributionItem<ProcessDescriptor>> items = e.processContributions;
			return items.toArray(new ContributionItem[items.size()]);
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object obj) {
			if (!(obj instanceof TreeItem))
				return false;
			TreeItem element = (TreeItem) obj;
			return element.processContributions.size() > 0;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object old, Object newInput) {
		}

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
		public Image getColumnImage(Object obj, int col) {
			if (col != 0)
				return null;
			ContributionItem<?> item = null;
			if (obj instanceof TreeItem) {
				TreeItem element = (TreeItem) obj;
				item = element.contribution;
			} else
				item = ContributionItem.class.cast(obj);
			return image.getForTable(item.share);
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object obj, int col) {
			if (obj instanceof TreeItem) {
				TreeItem e = (TreeItem) obj;
				return getLocationColumnText(e.contribution, col);
			}
			ContributionItem<ProcessDescriptor> item = ContributionItem.class
					.cast(obj);
			return getProcessColumnText(item, col);
		}

		private String getLocationColumnText(
				ContributionItem<Location> contribution, int col) {
			switch (col) {
			case LOCATION_COL:
				return contribution.item == null ? Messages.Other
						: Labels.getDisplayName(contribution.item);
			case PROCESS_COL:
				return "";
			case AMOUNT_COL:
				return Numbers.format(contribution.amount);
			case UNIT_COL:
				return unit;
			default:
				return null;
			}
		}

		private String getProcessColumnText(
				ContributionItem<ProcessDescriptor> contribution, int col) {
			switch (col) {
			case LOCATION_COL:
				return "";
			case PROCESS_COL:
				return contribution.item == null ? Messages.Other
						: Labels.getDisplayName(contribution.item);
			case AMOUNT_COL:
				return Numbers.format(contribution.amount);
			case UNIT_COL:
				return unit;
			default:
				return null;
			}
		}

	}

}
