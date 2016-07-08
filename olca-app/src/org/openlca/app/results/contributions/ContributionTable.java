package org.openlca.app.results.contributions;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.DQUIHelper;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.ModelType;
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

	private String[] columnLabels = { M.Contribution, M.Process, M.Amount, M.Unit };

	private String unit;
	private ModelType type;
	private long selectedModelId;
	private DQResult dqResult;

	public ContributionTable(Composite parent, ModelType type, DQResult dqResult) {
		super(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.MULTI);
		this.type = type;
		this.dqResult = dqResult;
		createColumns();
		Table table = this.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		setContentProvider(ArrayContentProvider.getInstance());
		Label label = new Label();
		setLabelProvider(label);
		createColumnSorters(label);
		double[] widths = { 0.2, 0.4, 0.2, 0.2 };
		if (dqResult != null && dqResult.exchangeSystem != null && type != ModelType.CURRENCY) {
			widths = DQUIHelper.adjustTableWidths(widths, dqResult.exchangeSystem);
		}
		Tables.bindColumnWidths(table, widths);
		Actions.bind(this, TableClipboard.onCopy(this));
	}

	public void setInput(List<ContributionItem<?>> items, String unit, long selectedModelId) {
		this.unit = unit; // do this before setting the input; otherwise the
							// labels will show the old unit
		this.selectedModelId = selectedModelId;
		setInput(items);
	}

	private void createColumns() {
		String[] columns = columnLabels;
		if (dqResult != null && dqResult.exchangeSystem != null && type != ModelType.CURRENCY) {
			columns = DQUIHelper.appendTableHeaders(columns, dqResult.exchangeSystem);
		}
		for (String label : columns) {
			TableViewerColumn column = new TableViewerColumn(this, SWT.NONE);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			column.getColumn().setText(label);
		}
	}

	private void createColumnSorters(Label p) {
		Viewers.sortByLabels(this, p, NAME, UNIT);
		Viewers.sortByDouble(this, (ContributionItem<?> i) -> i.share, CONTRIBUTION);
		Viewers.sortByDouble(this, (ContributionItem<?> i) -> i.amount, AMOUNT);
	}

	private class Label extends ColumnLabelProvider implements ITableLabelProvider, ITableColorProvider {

		private ContributionImage contributionImage = new ContributionImage(
				Display.getCurrent());

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof ContributionItem))
				return null;
			if (columnIndex != CONTRIBUTION)
				return null;
			ContributionItem<?> item = ContributionItem.class.cast(element);
			return contributionImage.getForTable(item.share);
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ContributionItem))
				return null;
			ContributionItem<ProcessDescriptor> item = ContributionItem.class.cast(element);
			switch (columnIndex) {
			case CONTRIBUTION:
				return Numbers.percent(item.share);
			case NAME:
				return Labels.getDisplayName(item.item);
			case AMOUNT:
				return Numbers.format(item.amount);
			case UNIT:
				return unit;
			default:
				ContributionItem<ProcessDescriptor> contribution = ContributionItem.class.cast(element);
				ProcessDescriptor process = contribution.item;
				int pos = columnIndex - 4;
				int[] quality = null;
				if (type == ModelType.FLOW) {
					quality = dqResult.getFlowQuality(process.getId(), selectedModelId);
				} else if (type == ModelType.IMPACT_CATEGORY) {
					quality = dqResult.getImpactQuality(process.getId(), selectedModelId);
				}
				return DQUIHelper.getLabel(pos, quality);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public Color getBackground(Object element, int columnIndex) {
			if (!(element instanceof ContributionItem))
				return null;
			if (columnIndex < 4)
				return null;
			ContributionItem<ProcessDescriptor> contribution = ContributionItem.class.cast(element);
			ProcessDescriptor process = contribution.item;
			int pos = columnIndex - 4;
			int[] quality = null;
			if (type == ModelType.FLOW) {
				quality = dqResult.getFlowQuality(process.getId(), selectedModelId);
			} else if (type == ModelType.IMPACT_CATEGORY) {
				quality = dqResult.getImpactQuality(process.getId(), selectedModelId);
			}
			if (quality == null)
				return null;
			return DQUIHelper.getColor(quality[pos], dqResult.exchangeSystem.getScoreCount());
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public void dispose() {
			contributionImage.dispose();
			super.dispose();
		}

	}
}
