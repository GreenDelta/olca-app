package org.openlca.app.editors.projects.results;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.ResultItemSelector;
import org.openlca.app.results.Sort;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.Contributions;
import org.openlca.core.results.ProjectResult;
import org.openlca.core.results.ResultItemView;
import org.openlca.util.Strings;

class ContributionSection extends LabelProvider implements TableSection,
	ResultItemSelector.SelectionHandler {

	private final ProjectResult result;
	private final ProjectVariant[] variants;

	private TableViewer table;
	private String unit;

	private ContributionSection(ProjectResult result) {
		this.result = result;
		this.variants = variantsOf(result);
	}

	static ContributionSection of(ProjectResult result) {
		return new ContributionSection(result);
	}

	@Override
	public void renderOn(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Result contributions");
		var comp = UI.sectionClient(section, tk, 1);
		UI.gridLayout(comp, 2);
		var items = ResultItemView.of(result);
		Sort.sort(items);
		ResultItemSelector.on(items)
			.withSelectionHandler(this)
			.create(comp, tk)
			.initWithEvent();

		var headers = new String[variants.length];
		var widths = new double[variants.length];
		var n = variants.length == 0 ? 1 : variants.length;
		for (int i = 0; i < variants.length; i++) {
			headers[i] = Strings.orEmpty(variants[i].name);
			widths[i] = 0.98 / n;
		}
		table = Tables.createViewer(comp, headers);
		table.setLabelProvider(this);
		Tables.bindColumnWidths(table, widths);
		Actions.bind(section, TableClipboard.onCopyAll(table));
		Actions.bind(table, TableClipboard.onCopySelected(table));
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		return null;
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof Cell[]))
			return null;
		var row = (Cell[]) obj;
		if (row.length <= col || row[col] == null)
			return null;
		var cell = row[col];
		var result = Numbers.format(cell.result, 2);
		if (unit != null) {
			result += " " + unit;
		}
		return result + " " + (cell.isRest()
			? "Others"
			: Labels.name(cell.process));
	}

	@Override
	public void onFlowSelected(EnviFlow flow) {
		unit = Labels.refUnit(flow);
		var builder = new RowBuilder(variants.length);
		for (int i = 0; i < variants.length; i++) {
			var variantResult = result.getResult(variants[i]);
			var contributions = Contributions.topWithRest(
				variantResult.getProcessContributions(flow), 9);
			for (int row = 0; row < contributions.size(); row++) {
				builder.add(i, row, Cell.of(contributions.get(row)));
			}
		}
		table.setInput(builder.get());
	}

	@Override
	public void onImpactSelected(ImpactDescriptor impact) {
		unit = impact.referenceUnit;
		var builder = new RowBuilder(variants.length);
		for (int i = 0; i < variants.length; i++) {
			var variantResult = result.getResult(variants[i]);
			var contributions = Contributions.topWithRest(
				variantResult.getProcessContributions(impact), 9);
			for (int row = 0; row < contributions.size(); row++) {
				builder.add(i, row, Cell.of(contributions.get(row)));
			}
		}
		table.setInput(builder.get());
	}

	@Override
	public void onCostsSelected(CostResultDescriptor cost) {
		unit = Labels.getReferenceCurrencyCode();
		var builder = new RowBuilder(variants.length);
		for (int i = 0; i < variants.length; i++) {
			var variantResult = result.getResult(variants[i]);
			var contributions = Contributions.topWithRest(
				variantResult.getProcessCostContributions(), 9);
			for (int row = 0; row < contributions.size(); row++) {
				builder.add(i, row, Cell.of(contributions.get(row)));
			}
		}
		table.setInput(builder.get());
	}

	private static class Cell {

		final CategorizedDescriptor process;
		final double result;

		Cell(CategorizedDescriptor process, double result) {
			this.process = process;
			this.result = result;
		}

		static Cell of(Contribution<CategorizedDescriptor> contribution) {
			return new Cell(contribution.item, contribution.amount);
		}

		boolean isRest() {
			return process == null;
		}
	}

	private static class RowBuilder {

		private final List<Cell[]> rows;
		private final int variantCount;

		private RowBuilder(int variantCount) {
			this.variantCount = variantCount;
			this.rows = new ArrayList<>();
		}

		void add(int variant, int row, Cell cell) {
			while (rows.size() <= row) {
				rows.add(new Cell[variantCount]);
			}
			var rowCells = rows.get(row);
			rowCells[variant] = cell;
		}

		List<Cell[]> get() {
			return rows;
		}
	}

}
