package org.openlca.app.results;

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
import org.openlca.app.results.LocationContributionPage.TreeInputElement;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.TreeClipboard;
import org.openlca.app.util.Trees;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;

/**
 * Table for showing the result contributions for locations of an analysis
 * result.
 */
class LocationContributionTable {

	private final int LOCATION_COL = 0;
	private final int PROCESS_COL = 1;
	private final int AMOUNT_COL = 2;
	private final int UNIT_COL = 3;
	private String[] COLUMN_LABELS = { Messages.Location, Messages.Process,
			Messages.Amount, Messages.Unit };

	private TreeViewer viewer;
	private String unit;

	public LocationContributionTable(Composite parent, boolean fullSize) {
		UI.gridLayout(parent, 1);
		viewer = new TreeViewer(parent);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		Tree tree = viewer.getTree();
		GridData gridData = UI.gridData(tree, true, true);
		if (!fullSize)
			gridData.heightHint = 150;
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		for (String col : COLUMN_LABELS) {
			TreeColumn column = new TreeColumn(tree, SWT.NONE);
			column.setText(col);
		}
		Trees.bindColumnWidths(tree, 0.35, 0.35, 0.15, 0.15);
		Actions.bind(viewer, TreeClipboard.onCopy(viewer));
	}

	public void setInput(List<TreeInputElement> contributions, String unit) {
		this.unit = unit;
		viewer.setInput(contributions);
	}

	private class ContentProvider implements ITreeContentProvider {

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement == null)
				return new Object[0];
			List<TreeInputElement> element = List.class.cast(inputElement);
			return element.toArray(new TreeInputElement[element.size()]);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof TreeInputElement))
				return new Object[0];
			TreeInputElement element = (TreeInputElement) parentElement;
			List<ContributionItem<ProcessDescriptor>> items = element
					.getProcessContributions();
			return items.toArray(new ContributionItem[items.size()]);
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object inputElement) {
			if (!(inputElement instanceof TreeInputElement))
				return false;
			TreeInputElement element = (TreeInputElement) inputElement;
			return element.getProcessContributions().size() > 0;
		}

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// do nothing
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
		public Image getColumnImage(Object element, int col) {
			if (col != 0)
				return null;
			ContributionItem<?> contribution = null;
			if (element instanceof TreeInputElement) {
				TreeInputElement inputElement = (TreeInputElement) element;
				contribution = inputElement.getContribution();
			} else
				contribution = ContributionItem.class.cast(element);
			return image.getForTable(contribution.getShare());
		}

		@SuppressWarnings("unchecked")
		@Override
		public String getColumnText(Object element, int col) {
			if (element instanceof TreeInputElement) {
				TreeInputElement inputElement = (TreeInputElement) element;
				return getLocationColumnText(inputElement.getContribution(),
						col);
			}
			ContributionItem<ProcessDescriptor> contribution = ContributionItem.class
					.cast(element);
			return getProcessColumnText(
					(ContributionItem<ProcessDescriptor>) contribution, col);
		}

		private String getLocationColumnText(
				ContributionItem<Location> contribution, int col) {
			switch (col) {
			case LOCATION_COL:
				return contribution.getItem() == null ? Messages.Other
						: contribution.getItem().getName();
			case PROCESS_COL:
				return "";
			case AMOUNT_COL:
				return Numbers.format(contribution.getAmount());
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
