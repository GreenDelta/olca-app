package org.openlca.app.results.requirements;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Actions;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.trees.TreeClipboard;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.results.ContributionResult;

/**
 * The total requirements section that is shown on the TotalFlowResultPage.
 */
public class TotalRequirementsSection {

	private final ContributionResult result;
	private final DQResult dqResult;
	private final Costs costs;

	TreeViewer tree;

	public TotalRequirementsSection(ContributionResult result, DQResult dqResult) {
		this.result = result;
		costs = !result.hasCosts()
			? Costs.NONE
			: result.totalCosts() >= 0
			? Costs.NET_COSTS
			: Costs.ADDED_VALUE;
		this.dqResult = dqResult;
	}

	public void create(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.TotalRequirements);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		var searchComp = tk.createComposite(comp);
		UI.gridData(searchComp, true, false);
		UI.gridLayout(searchComp, 2, 10, 0);
		tk.createLabel(searchComp, M.Search);
		var searchText = tk.createText(searchComp, "");
		UI.gridData(searchText, true, false);

		var label = new LabelProvider(dqResult, costs);
		tree = Trees.createViewer(comp, columnLabels(), label);
		tree.getTree().setLinesVisible(true);
		var model = new TreeModel(result, costs);
		tree.setContentProvider(model);
		tree.setFilters(new SearchFilter(this, model, searchText));
		Trees.bindColumnWidths(tree.getTree(), DQUI.MIN_COL_WIDTH, columnWidths());

		var numericColumns = costs == Costs.NONE || costs == null
			? new int[]{2}
			: new int[]{2, 4};
		for (int col : numericColumns) {
			tree.getTree().getColumns()[col].setAlignment(SWT.RIGHT);
		}

		addSorters(tree, label);
		addActions(tree);
		renderTotalCosts(comp, tk);
	}

	public void fill() {
		if (tree == null)
			return;
		tree.setInput(result);
		expandFirst();
	}

	/**
	 * Expand the first path in the tree.
	 */
	void expandFirst() {
		if (tree == null)
			return;
		var items = tree.getTree().getItems();
		while (items != null && items.length > 0) {
			var first = items[0];
			var content = first.getData();
			if (content instanceof ProviderItem)
				break;
			first.setExpanded(true);
			for (int i = 1; i < items.length; i++) {
				items[i].setExpanded(false);
			}
			tree.refresh();
			items = first.getItems();
		}
	}

	private void addActions(TreeViewer tree) {
		Action onOpen = Actions.onOpen(() -> {
			Item item = Viewers.getFirstSelected(tree);
			if (item == null)
				return;
			TechFlow product = null;
			if (item.isProvider()) {
				product = item.asProvider().product;
			} else if (item.isChild()) {
				product = item.asChild().product;
			}
			if (product != null) {
				App.open(product.provider());
			}
		});
		Actions.bind(tree, onOpen, TreeClipboard.onCopy(tree));
		Trees.onDoubleClick(tree, e -> onOpen.run());
	}

	private void addSorters(TreeViewer tree, LabelProvider label) {
		Viewers.sortByLabels(tree, label, 0, 1, 3);
		Viewers.sortByDouble(tree, (Item i) -> {
			if (i.isProvider())
				return i.asProvider().amount;
			if (i.isChild())
				return i.asChild().amount;
			return 0.0;
		}, 2);
		if (costs != Costs.NONE) {
			Viewers.sortByDouble(tree, (Item i) -> i.isProvider()
					? i.asProvider().costValue
					: 0.0,
				4);
		}
		if (DQUI.displayProcessQuality(dqResult)) {
			int startCol = costs == Costs.NONE ? 4 : 5;
			for (int i = 0; i < dqResult.setup.processSystem.indicators.size(); i++) {
				Viewers.sortByDouble(tree, label, i + startCol);
			}
		}
	}

	private void renderTotalCosts(Composite comp, FormToolkit tk) {
		if (costs == Costs.NONE)
			return;
		String label = costs == Costs.NET_COSTS
			? M.TotalNetcosts
			: M.TotalAddedValue;
		double v = costs == Costs.NET_COSTS
			? result.totalCosts()
			: result.totalCosts() == 0
			? 0
			: -result.totalCosts();
		var currency = new CurrencyDao(Database.get())
			.getReferenceCurrency();
		var symbol = currency != null && currency.code != null
			? currency.code
			: "?";
		var value = Numbers.decimalFormat(v, 2) + " " + symbol;
		tk.createLabel(comp, label + ": " + value)
			.setFont(UI.boldFont());
	}

	private String[] columnLabels() {
		List<String> b = new ArrayList<>();
		b.add(M.Process);
		b.add(M.Product);
		b.add(M.Amount);
		b.add(M.Unit);
		if (costs == Costs.ADDED_VALUE) {
			b.add(M.AddedValue);
		} else if (costs == Costs.NET_COSTS) {
			b.add(M.NetCosts);
		}
		var labels = b.toArray(new String[0]);
		return DQUI.displayProcessQuality(dqResult)
			? DQUI.appendTableHeaders(labels, dqResult.setup.processSystem)
			: labels;
	}

	private double[] columnWidths() {
		double[] widths = costs == Costs.NONE
			? new double[]{.4, .2, .2, .2}
			: new double[]{.4, .2, .2, .1, .1};
		if (!DQUI.displayProcessQuality(dqResult))
			return widths;
		return DQUI.adjustTableWidths(widths, dqResult.setup.processSystem);
	}

}
