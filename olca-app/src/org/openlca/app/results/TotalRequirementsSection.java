package org.openlca.app.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.results.ContributionResult;

/**
 * The total requirements section that is shown on the TotalFlowResultPage.
 */
class TotalRequirementsSection {

	private final ContributionResult result;
	private final DQResult dqResult;
	private final Costs costs;
	private String currencySymbol;

	private TableViewer table;

	TotalRequirementsSection(ContributionResult result, DQResult dqResult) {
		this.result = result;
		costs = !result.hasCostResults()
			? Costs.NONE
			: result.totalCosts >= 0
			? Costs.NET_COSTS
			: Costs.ADDED_VALUE;
		this.dqResult = dqResult;
	}

	void create(Composite body, FormToolkit tk) {
		Section section = UI.section(body, tk, M.TotalRequirements);
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		Label label = new Label();
		table = Tables.createViewer(comp, columnLabels(), label);
		Tables.bindColumnWidths(table, DQUI.MIN_COL_WIDTH, columnWidths());
		Viewers.sortByLabels(table, label, 0, 1, 3);
		Viewers.sortByDouble(table, (Item i) -> i.amount, 2);
		if (costs != Costs.NONE)
			Viewers.sortByDouble(table, (Item i) -> i.costValue, 4);
		if (DQUI.displayProcessQuality(dqResult)) {
			int startCol = costs == Costs.NONE ? 4 : 5;
			for (int i = 0; i < dqResult.setup.processSystem.indicators.size(); i++) {
				Viewers.sortByDouble(table, label, i + startCol);
			}
		}
		for (int col : numberColumns()) {
			table.getTable().getColumns()[col].setAlignment(SWT.RIGHT);
		}

		Action onOpen = Actions.onOpen(() -> {
			Item item = Viewers.getFirstSelected(table);
			if (item != null) {
				App.openEditor(item.product.process);
			}
		});
		Actions.bind(table, onOpen, TableClipboard.onCopy(table));
		Tables.onDoubleClick(table, e -> onOpen.run());
		createCostSum(comp, tk);
	}

	private void createCostSum(Composite comp, FormToolkit tk) {
		if (costs == Costs.NONE)
			return;
		double v = result.totalCosts;
		String s;
		String c;
		if (costs == Costs.NET_COSTS) {
			s = M.TotalNetcosts;
			c = formatCosts(v);
		} else {
			s = M.TotalAddedValue;
			c = formatCosts(v == 0 ? 0 : -v);
		}

		Control label = tk.createLabel(comp, s + ": " + c);
		label.setFont(UI.boldFont());
	}

	void fill() {
		if (table == null)
			return;
		table.setInput(createItems());
	}

	private String[] columnLabels() {
		List<String> b = new ArrayList<>();
		b.add(M.Process);
		b.add(M.Product);
		b.add(M.Amount);
		b.add(M.Unit);
		if (costs == Costs.ADDED_VALUE)
			b.add(M.AddedValue);
		else if (costs == Costs.NET_COSTS)
			b.add(M.NetCosts);
		String[] columnLabels = b.toArray(new String[0]);
		if (!DQUI.displayProcessQuality(dqResult))
			return columnLabels;
		return DQUI.appendTableHeaders(columnLabels, dqResult.setup.processSystem);
	}

	private int[] numberColumns() {
		if (costs == Costs.NONE || costs == null)
			return new int[]{2};
		return new int[]{2, 4};
	}

	private double[] columnWidths() {
		double[] widths = costs == Costs.NONE
			? new double[]{.4, .2, .2, .2}
			: new double[]{.4, .2, .2, .1, .1};
		if (!DQUI.displayProcessQuality(dqResult))
			return widths;
		return DQUI.adjustTableWidths(widths, dqResult.setup.processSystem);
	}

	private List<Item> createItems() {
		if (result == null)
			return Collections.emptyList();
		double[] tr = result.totalRequirements;
		if (tr == null)
			return Collections.emptyList();
		List<Item> items = new ArrayList<>();
		for (int i = 0; i < tr.length; i++) {
			items.add(new Item(i, tr[i]));
		}
		calculateCostChares(items);
		items.sort((a, b) -> -Double.compare(a.amount, b.amount));
		return items;
	}

	private void calculateCostChares(List<Item> items) {
		if (costs == Costs.NONE)
			return;
		double max = 0;
		for (Item item : items) {
			max = Math.max(max, item.costValue);
		}
		if (max == 0)
			return;
		for (Item item : items) {
			item.costShare = item.costValue / max;
		}
	}

	private String formatCosts(double value) {
		if (currencySymbol == null) {
			var dao = new CurrencyDao(Database.get());
			var ref = dao.getReferenceCurrency();
			currencySymbol = ref == null
				? "?"
				: ref.code != null
					? ref.code
					: ref.name;
		}
		return Numbers.decimalFormat(value, 2) + " " + currencySymbol;
	}

	private enum Costs {
		ADDED_VALUE, NET_COSTS, NONE
	}

	private class Item {

		ProcessProduct product;
		double amount;
		double costValue;
		double costShare;

		String flow;
		String unit;
		FlowType flowtype;

		Item(int idx, double amount) {
			this.amount = amount;
			var techIdx = result.techIndex;
			product = techIdx.getProviderAt(idx);
			var flow = product.flow;
			if (flow != null) {
				this.flow = Labels.name(flow);
				this.unit = Labels.refUnit(flow);
				this.flowtype = flow.flowType;
			}
			initCostValue(idx);
		}

		private void initCostValue(int idx) {
			if (costs == Costs.NONE)
				return;
			if (result == null)
				return;
			if (idx >= 0) {
				double v = result.provider.directCostsOf(idx);
				costValue = costs == Costs.NET_COSTS
					? v
					: v != 0 ? -v : 0;
			}
		}

	}

	private class Label extends DQLabelProvider {

		private final ContributionImage costImage = new ContributionImage();

		public Label() {
			super(dqResult, dqResult != null
				? dqResult.setup.processSystem
				: null, costs == Costs.NONE ? 4 : 5);
		}

		@Override
		public void dispose() {
			costImage.dispose();
			super.dispose();
		}

		@Override
		public Image getImage(Object obj, int col) {
			if (!(obj instanceof Item))
				return null;
			Item item = (Item) obj;
			switch (col) {
				case 0:
					return Images.get(ModelType.PROCESS);
				case 1:
					return Images.get(item.flowtype);
				case 4:
					if (costs == Costs.NONE)
						return null;
					return costImage.getForTable(item.costShare);
				default:
					return null;
			}
		}

		@Override
		public String getText(Object obj, int col) {
			if (!(obj instanceof Item))
				return null;
			Item item = (Item) obj;
			switch (col) {
				case 0:
					return Labels.of(item.product);
				case 1:
					return item.flow;
				case 2:
					double val = item.flowtype == FlowType.WASTE_FLOW
						? -item.amount
						: item.amount;
					return Numbers.format(val);
				case 3:
					return item.unit;
				case 4:
					return formatCosts(item.costValue);
				default:
					return null;
			}
		}

		@Override
		protected int[] getQuality(Object obj) {
			if (!(obj instanceof Item))
				return null;
			Item item = (Item) obj;
			return dqResult.get(item.product);
		}
	}
}
