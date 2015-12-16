package org.openlca.app.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.matrix.ProductIndex;
import org.openlca.core.model.Currency;
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
	private Costs costs;

	private TableViewer table;

	TotalRequirementsSection(SimpleResultProvider<?> result) {
		this.result = result;
		if (!result.hasCostResults())
			costs = Costs.NONE;
		else {
			costs = result.getTotalCostResult() >= 0
					? Costs.NET_COSTS : Costs.ADDED_VALUE;
		}
	}

	void create(Composite body, FormToolkit tk) {
		Section section = UI.section(body, tk, "#Total requirements");
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		table = Tables.createViewer(comp, columnLables());
		Tables.bindColumnWidths(table, columnWidths());
		Label label = new Label();
		table.setLabelProvider(label);
		Viewers.sortByLabels(table, label, 0, 1, 3);
		Viewers.sortByDouble(table, (Item i) -> i.amount, 2);
		Actions.bind(table, TableClipboard.onCopy(table));
		Tables.onDoubleClick(table, e -> {
			Item item = Viewers.getFirstSelected(table);
			if (item != null) {
				App.openEditor(cache.get(ProcessDescriptor.class, item.processId));
			}
		});
	}

	void fill() {
		if (table == null)
			return;
		table.setInput(createItems());
	}

	private String[] columnLables() {
		List<String> b = new ArrayList<>();
		b.add(Messages.Process);
		b.add(Messages.Product);
		b.add(Messages.Amount);
		b.add(Messages.Unit);
		if (costs == Costs.ADDED_VALUE)
			b.add("#Added value");
		else if (costs == Costs.NET_COSTS)
			b.add("#Net costs");
		return b.toArray(new String[b.size()]);
	}

	private double[] columnWidths() {
		if (costs == Costs.NONE)
			return new double[] { .4, .2, .2, .2 };
		else
			return new double[] { .4, .2, .2, .1, .1 };
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

	private enum Costs {
		ADDED_VALUE, NET_COSTS, NONE
	}

	private class Item {

		long processId;
		String process;
		String product;
		double amount;
		String unit;
		double costValue;
		double costShare;

		Item(int idx, double amount) {
			this.amount = amount;
			init(idx);
		}

		private void init(int idx) {
			ProductIndex productIdx = result.result.productIndex;
			if (productIdx == null)
				return;
			setProcessProduct(productIdx, idx);
			setCostValue(idx);
		}

		private void setProcessProduct(ProductIndex productIdx, int idx) {
			LongPair lp = productIdx.getProductAt(idx);
			if (lp == null)
				return;
			ProcessDescriptor process = cache.get(ProcessDescriptor.class,
					lp.getFirst());
			if (process != null) {
				this.processId = process.getId();
				this.process = Labels.getDisplayName(process);
			}
			FlowDescriptor flow = cache.get(FlowDescriptor.class, lp.getSecond());
			if (flow != null) {
				this.product = Labels.getDisplayName(flow);
				this.unit = Labels.getRefUnit(flow, cache);
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

	private class Label extends BaseLabelProvider implements ITableLabelProvider {

		private String currencySymbol;
		private ContributionImage costImage = new ContributionImage(
				UI.shell().getDisplay());

		@Override
		public void dispose() {
			costImage.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Item))
				return null;
			switch (col) {
			case 0:
				return ImageType.PROCESS_ICON.get();
			case 1:
				return ImageType.FLOW_ICON.get();
			case 4:
				return costImage.getForTable(((Item) obj).costShare);
			default:
				return null;
			}
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Item))
				return null;
			Item item = (Item) obj;
			switch (col) {
			case 0:
				return item.process;
			case 1:
				return item.product;
			case 2:
				return Numbers.format(item.amount);
			case 3:
				return item.unit;
			case 4:
				return Numbers.format(item.costValue) + " " + getCurrency();
			default:
				return null;
			}
		}

		private String getCurrency() {
			if (currencySymbol != null)
				return currencySymbol;
			try {
				CurrencyDao dao = new CurrencyDao(Database.get());
				Currency ref = dao.getReferenceCurrency();
				currencySymbol = ref.code != null ? ref.code : ref.getName();
			} catch (Exception e) {
				currencySymbol = "?";
			}
			return currencySymbol;
		}

	}

}
