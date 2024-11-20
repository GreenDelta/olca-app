package org.openlca.app.results.analysis.groups;

import java.util.ArrayList;
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
import org.openlca.app.util.Colors;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.ResultItemOrder;
import org.openlca.core.results.agroups.AnalysisGroupResult;
import org.openlca.util.Strings;

class ChartSection {

	private final ResultItemOrder items;
	private final List<AnalysisGroup> groups;

	private AnalysisGroupResult result;
	private String unit;
	private ResultItemSelector selector;
	private TableViewer table;

	ChartSection(ResultEditor editor, List<AnalysisGroup> groups) {
		this.items = editor.items();
		this.groups = groups;
	}

	void render(Composite body, FormToolkit tk) {

		var comp = UI.formSection(body, tk, M.ResultContributions, 1);
		var top = tk.createComposite(comp);
		UI.gridLayout(top, 2);
		selector = ResultItemSelector
				.on(items)
				.withSelectionHandler(new Handler())
				.create(top, tk);

		table = Tables.createViewer(comp, "Group", "Result", "Unit");
		Tables.bindColumnWidths(table, 0.4, 0.4, 0.2);
		table.setLabelProvider(new TableLabel());
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

	private class Handler implements SelectionHandler {

		@Override
		public void onFlowSelected(EnviFlow flow) {
			if (flow == null || result == null)
				return;
			unit = Labels.refUnit(flow);
			var map = result.getResultsOf(flow);
			table.setInput(Item.allOf(groups, map));
		}

		@Override
		public void onImpactSelected(ImpactDescriptor impact) {
			if (impact == null || result == null || table == null)
				return;
			unit = impact.referenceUnit;
			var map = result.getResultsOf(impact);
			table.setInput(Item.allOf(groups, map));
		}

		@Override
		public void onCostsSelected(CostResultDescriptor cost) {
			if (cost == null || result == null)
				return;
			unit = Labels.getReferenceCurrencyCode();
			var map = cost.forAddedValue
					? result.getAddedValueResults()
					: result.getCostResults();
			table.setInput(Item.allOf(groups, map));
		}
	}

	private class TableLabel extends LabelProvider
			implements ITableLabelProvider {

		private final ContributionImage image = new ContributionImage()
				.withFullWidth(20);

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col != 0 || !(obj instanceof Item item))
				return null;
			if (Strings.nullOrEmpty(item.group.color))
				return null;
			var color = Colors.fromHex(item.group.color);
			return color != null
					? image.get(1.0, color)
					: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			return switch (col) {
				case 0 -> item.name();
				case 1 -> Numbers.format(item.value);
				case 2 -> unit;
				default -> null;
			};
		}
	}

	private record Item(AnalysisGroup group, double value) {

		String name() {
			return group.name;
		}

		static List<Item> allOf(
				List<AnalysisGroup> groups, Map<String, Double> map
		) {
			var items = new ArrayList<Item>(groups.size());
			for (var g : groups) {
				var value = map.get(g.name);
				items.add(new Item(g, value != null ? value : 0d));
			}
			return items;
		}
	}
}
