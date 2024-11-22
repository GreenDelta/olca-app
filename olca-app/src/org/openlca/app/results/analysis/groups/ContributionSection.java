package org.openlca.app.results.analysis.groups;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.components.ResultItemSelector;
import org.openlca.app.components.ResultItemSelector.SelectionHandler;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.ResultItemOrder;
import org.openlca.core.results.agroups.AnalysisGroupResult;

class ContributionSection {

	private final ResultItemOrder items;
	private final List<AnalysisGroup> groups;

	private AnalysisGroupResult result;
	private String unit;
	private ResultItemSelector selector;
	private TableViewer table;

	ContributionSection(ResultEditor editor, List<AnalysisGroup> groups) {
		this.items = editor.items();
		this.groups = groups;
	}

	void render(Composite body, FormToolkit tk) {

		var parent = UI.formSection(body, tk, M.ResultContributions, 1);
		var top = tk.createComposite(parent);
		UI.gridLayout(top, 2);
		selector = ResultItemSelector
				.on(items)
				.withSelectionHandler(new Handler())
				.create(top, tk);

		var sub = tk.createComposite(parent);
		UI.fillHorizontal(sub);
		UI.gridLayout(sub, 1);
		table = Tables.createViewer(sub, "Group", "Result", "Unit");
		Tables.bindColumnWidths2(table, 0.4, 0.4, 0.2);
		table.setLabelProvider(new TableLabel());
		Actions.bind(table, TableClipboard.onCopySelected(table));
	}

	void setResult(AnalysisGroupResult result) {
		this.result = result;
		if (items.hasImpacts()) {
			selector.selectWithEvent(items.impacts().getFirst());
		} else if (items.hasEnviFlows()) {
			selector.selectWithEvent(items.enviFlows().getFirst());
		} else if (items.hasCosts()) {
			selector.selectWithEvent(CostResultDescriptor.netCosts());
		}
	}

	private void setResult(Map<String, Double> map, String unit) {
		this.unit = unit;
		if (table == null)
			return;
		var values = GroupValue.allOf(groups, map);
		table.setInput(values);
	}

	private class Handler implements SelectionHandler {

		@Override
		public void onFlowSelected(EnviFlow flow) {
			if (flow == null || result == null)
				return;
			setResult(
					result.getResultsOf(flow),
					Labels.refUnit(flow));
		}

		@Override
		public void onImpactSelected(ImpactDescriptor impact) {
			if (impact == null || result == null)
				return;
			setResult(
					result.getResultsOf(impact),
					impact.referenceUnit);
		}

		@Override
		public void onCostsSelected(CostResultDescriptor cost) {
			if (cost == null || result == null)
				return;
			var map = cost.forAddedValue
					? result.getAddedValueResults()
					: result.getCostResults();
			setResult(map, Labels.getReferenceCurrencyCode());
		}
	}

	private class TableLabel extends LabelProvider
			implements ITableLabelProvider {

		private final ContributionImage image = new ContributionImage();

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0 && obj instanceof GroupValue item
					? image.get(item.share())
					: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof GroupValue item))
				return null;
			return switch (col) {
				case 0 -> item.name();
				case 1 -> Numbers.format(item.value());
				case 2 -> unit;
				default -> null;
			};
		}
	}

}
