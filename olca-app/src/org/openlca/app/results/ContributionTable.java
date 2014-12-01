package org.openlca.app.results;

import java.util.List;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.openlca.app.Messages;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.TableColumnSorter;
import org.openlca.app.util.Tables;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;

/**
 * Table viewer for process contributions to a LCIA category or flow.
 */
class ContributionTable extends TableViewer {

	private final int CONTRIBUTION = 0;
	private final int NAME = 1;
	private final int AMOUNT = 2;
	private final int UNIT = 3;

	private String[] columnLabels = { Messages.Contribution, Messages.Process,
			Messages.Amount, Messages.Unit };

	private String unit;

	public ContributionTable(Composite parent) {
		super(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.MULTI);
		createColumns();
		Table table = this.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		setContentProvider(ArrayContentProvider.getInstance());
		Label label = new Label();
		setLabelProvider(label);
		createColumnSorters(label);
		Tables.bindColumnWidths(table, 0.2, 0.4, 0.2, 0.2);
		Actions.bind(this, TableClipboard.onCopy(this));
	}

	public void setInput(List<ContributionItem<?>> items, String unit) {
		this.unit = unit; // do this before setting the input; otherwise the
							// labels will show the old unit
		setInput(items);
	}

	private void createColumns() {
		for (String label : columnLabels) {
			TableViewerColumn column = new TableViewerColumn(this, SWT.NONE);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			column.getColumn().setText(label);
		}
	}

	private void createColumnSorters(Label p) {
		TableColumnSorter<?> first = ContributionSorter.forShare(CONTRIBUTION);
		first.setAscending(false);
		Tables.registerSorters(this,
				first,
				new TableColumnSorter<>(ContributionItem.class, NAME, p),
				ContributionSorter.forAmount(AMOUNT),
				new TableColumnSorter<>(ContributionItem.class, UNIT, p));
	}

	private class Label extends ColumnLabelProvider implements
			ITableLabelProvider {

		private ContributionImage contributionImage = new ContributionImage(
				Display.getCurrent());

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof ContributionItem))
				return null;
			if (columnIndex != CONTRIBUTION)
				return null;
			ContributionItem<?> item = ContributionItem.class.cast(element);
			return contributionImage.getForTable(item.getShare());
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ContributionItem))
				return null;
			ContributionItem<ProcessDescriptor> item = ContributionItem.class
					.cast(element);
			switch (columnIndex) {
			case CONTRIBUTION:
				return Numbers.percent(item.getShare());
			case NAME:
				return Labels.getDisplayName(item.getItem());
			case AMOUNT:
				return Numbers.format(item.getAmount());
			case UNIT:
				return unit;
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
}
