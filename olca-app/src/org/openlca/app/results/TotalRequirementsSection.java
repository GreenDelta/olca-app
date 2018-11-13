package org.openlca.app.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.matrix.TechIndex;
import org.openlca.core.model.Currency;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.SimpleResultProvider;

/**
 * The total requirements section that is shown on the TotalFlowResultPage.
 */
class TotalRequirementsSection {

	private EntityCache cache = Cache.getEntityCache();
	private SimpleResultProvider<?> result;
	private DQResult dqResult;
	private Costs costs;
	private String currencySymbol;
	private Map<Long, ProcessDescriptor> processes = new HashMap<>();

	private TableViewer table;

	TotalRequirementsSection(SimpleResultProvider<?> result, DQResult dqResult) {
		this.result = result;
		for (ProcessDescriptor desc : result.getProcessDescriptors())
			processes.put(desc.getId(), desc);
		if (!result.hasCostResults())
			costs = Costs.NONE;
		else
			costs = result.getTotalCostResult() >= 0 ? Costs.NET_COSTS : Costs.ADDED_VALUE;
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
			for (int i = 0; i < dqResult.setup.processDqSystem.indicators.size(); i++) {
				Viewers.sortByDouble(table, label, i + startCol);
			}
		}
		for (int col : numberColumns()) {
			table.getTable().getColumns()[col].setAlignment(SWT.RIGHT);
		}
		Actions.bind(table, TableClipboard.onCopy(table));
		Tables.onDoubleClick(table, e -> {
			Item item = Viewers.getFirstSelected(table);
			if (item != null) {
				App.openEditor(item.process);
			}
		});
		createCostSum(comp, tk);
	}

	private void createCostSum(Composite comp, FormToolkit tk) {
		if (costs == Costs.NONE)
			return;
		double v = result.getTotalCostResult();
		String s;
		String c;
		if (costs == Costs.NET_COSTS) {
			s = M.TotalNetcosts;
			c = asCosts(v);
		} else {
			s = M.TotalAddedValue;
			c = asCosts(v == 0 ? 0 : -v);
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
		String[] columnLabels = b.toArray(new String[b.size()]);
		if (!DQUI.displayProcessQuality(dqResult))
			return columnLabels;
		return DQUI.appendTableHeaders(columnLabels, dqResult.setup.processDqSystem);
	}

	private int[] numberColumns() {
		if (costs == Costs.NONE || costs == null)
			return new int[] { 2 };
		return new int[] { 2, 4 };
	}

	private double[] columnWidths() {
		double[] widths = null;
		if (costs == Costs.NONE)
			widths = new double[] { .4, .2, .2, .2 };
		else
			widths = new double[] { .4, .2, .2, .1, .1 };
		if (!DQUI.displayProcessQuality(dqResult))
			return widths;
		return DQUI.adjustTableWidths(widths, dqResult.setup.processDqSystem);
	}

	private List<Item> createItems() {
		if (result == null || result.result == null)
			return Collections.emptyList();
		double[] tr = result.result.totalRequirements;
		if (tr == null)
			return Collections.emptyList();
		List<Item> items = new ArrayList<>();
		for (int i = 0; i < tr.length; i++) {
			items.add(new Item(i, tr[i]));
		}
		calculateCostChares(items);
		Collections.sort(items, (a, b) -> -Double.compare(a.amount, b.amount));
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

	private String asCosts(double value) {
		if (currencySymbol == null) {
			try {
				CurrencyDao dao = new CurrencyDao(Database.get());
				Currency ref = dao.getReferenceCurrency();
				currencySymbol = ref.code != null ? ref.code : ref.getName();
			} catch (Exception e) {
				currencySymbol = "?";
			}
		}
		return Numbers.decimalFormat(value, 2) + " " + currencySymbol;
	}

	private enum Costs {
		ADDED_VALUE, NET_COSTS, NONE
	}

	private class Item {

		ProcessDescriptor process;
		String product;
		double amount;
		String unit;
		double costValue;
		double costShare;
		FlowType flowtype;

		Item(int idx, double amount) {
			this.amount = amount;
			init(idx);
		}

		private void init(int idx) {
			TechIndex index = result.result.techIndex;
			if (index == null)
				return;
			setProcessProduct(index, idx);
			setCostValue(idx);
		}

		private void setProcessProduct(TechIndex techIdx, int idx) {
			LongPair lp = techIdx.getProviderAt(idx);
			if (lp == null)
				return;
			ProcessDescriptor process = processes.get(lp.getFirst());
			if (process != null) {
				this.process = process;
			}
			FlowDescriptor flow = cache.get(FlowDescriptor.class, lp.getSecond());
			if (flow != null) {
				this.product = Labels.getDisplayName(flow);
				this.unit = Labels.getRefUnit(flow, cache);
				this.flowtype = flow.getFlowType();
			}
		}

		private void setCostValue(int idx) {
			if (costs == Costs.NONE)
				return;
			if (!(result instanceof ContributionResultProvider))
				return;
			ContributionResultProvider<?> crp = (ContributionResultProvider<?>) result;
			double[] vals = crp.result.singleCostResults;
			if (vals.length > idx && idx >= 0) {
				double v = vals[idx];
				costValue = costs == Costs.NET_COSTS ? v : v != 0 ? -v : 0;
			}
		}

	}

	private class Label extends DQLabelProvider {

		private ContributionImage costImage = new ContributionImage(
				UI.shell().getDisplay());

		public Label() {
			super(dqResult, dqResult != null
					? dqResult.setup.processDqSystem
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
				return Labels.getDisplayName(item.process);
			case 1:
				return item.product;
			case 2:
				double val = item.flowtype == FlowType.WASTE_FLOW
						? -item.amount
						: item.amount;
				return Numbers.format(val);
			case 3:
				return item.unit;
			case 4:
				return asCosts(item.costValue);
			default:
				return null;
			}
		}

		@Override
		protected double[] getQuality(Object obj) {
			if (!(obj instanceof Item))
				return null;
			Item item = (Item) obj;
			return dqResult.get(item.process);
		}
	}
}
