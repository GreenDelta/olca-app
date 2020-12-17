package org.openlca.app.results.requirements;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
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
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.results.ContributionResult;

/**
 * The total requirements section that is shown on the TotalFlowResultPage.
 */
public class TotalRequirementsSection {

	private final ContributionResult result;
	private final DQResult dqResult;
	private final Costs costs;
	private String currencySymbol;

	private TreeViewer tree;

	public TotalRequirementsSection(ContributionResult result, DQResult dqResult) {
		this.result = result;
		costs = !result.hasCostResults()
			? Costs.NONE
			: result.totalCosts >= 0
			? Costs.NET_COSTS
			: Costs.ADDED_VALUE;
		this.dqResult = dqResult;
	}

	public void create(Composite body, FormToolkit tk) {
		Section section = UI.section(body, tk, M.TotalRequirements);
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		var label = new LabelProvider(dqResult, costs);
		tree = Trees.createViewer(comp, columnLabels(), label);
		tree.getTree().setLinesVisible(true);
		tree.setContentProvider(new ContentProvider());
		Trees.bindColumnWidths(tree.getTree(), DQUI.MIN_COL_WIDTH, columnWidths());
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
		for (int col : numberColumns()) {
			tree.getTree().getColumns()[col].setAlignment(SWT.RIGHT);
		}

		Action onOpen = Actions.onOpen(() -> {
			Item item = Viewers.getFirstSelected(tree);
			if (item == null)
				return;
			ProcessProduct product = null;
			if (item.isProvider()) {
				product = item.asProvider().product;
			} else if (item.isChild()) {
				product = item.asChild().product;
			}
			if (product != null) {
				App.openEditor(product.process);
			}
		});

		Actions.bind(tree, onOpen, TreeClipboard.onCopy(tree));
		Trees.onDoubleClick(tree, e -> onOpen.run());
		createCostSum(comp, tk);
	}

	private void createCostSum(Composite comp, FormToolkit tk) {
		if (costs == Costs.NONE)
			return;
		String label = costs == Costs.NET_COSTS
			? M.TotalNetcosts
			: M.TotalAddedValue;
		double v = result.totalCosts;
		String value = costs == Costs.NET_COSTS
			? formatCosts(v)
			: formatCosts(v == 0 ? 0 : -v);
		tk.createLabel(comp, label + ": " + value)
			.setFont(UI.boldFont());
	}

	public void fill() {
		if (tree == null)
			return;
		tree.setInput(result);
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

	private class ContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object input) {
			if (!(input instanceof ContributionResult))
				return new Object[0];
			return ProviderItem.allOf(result, costs).toArray();
		}

		@Override
		public Object[] getChildren(Object elem) {
			if (!(elem instanceof Item))
				return new Object[0];
			var item = (Item) elem;
			return item.isProvider()
				? ChildItem.allOf(item.asProvider(), result).toArray()
				: new Object[0];
		}

		@Override
		public boolean hasChildren(Object elem) {
			if (!(elem instanceof Item))
				return false;
			var item = (Item) elem;
			return item.isProvider();
		}

		@Override
		public Object getParent(Object elem) {
			return null;
		}
	}

}
