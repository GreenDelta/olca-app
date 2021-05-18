package org.openlca.app.editors.projects.results;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;

import gnu.trove.map.hash.TLongObjectHashMap;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.components.ResultItemSelector;
import org.openlca.app.results.Sort;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.Contributions;
import org.openlca.core.results.ProjectResult;
import org.openlca.core.results.ResultItemView;
import org.openlca.util.Strings;

class ContributionSection extends LabelProvider implements TableSection,
	ResultItemSelector.SelectionHandler {

	private final ProjectResult result;
	private final ProjectVariant[] variants;
	private TableViewer table;
	private ContributionImage image;

	private String unit;
	private int count;
	private String query;
	private double absMax;
	private List<List<Cell>> cells;

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
		UI.gridLayout(comp, 1);

		var selectorComp = tk.createComposite(comp);
		UI.gridLayout(selectorComp, 2, 5, 0);
		var items = ResultItemView.of(result);
		Sort.sort(items);
		var selector = ResultItemSelector.on(items)
			.withSelectionHandler(this)
			.create(selectorComp, tk);

		var headers = new String[variants.length];
		var widths = new double[variants.length];
		var n = variants.length == 0 ? 1 : variants.length;
		for (int i = 0; i < variants.length; i++) {
			headers[i] = Strings.orEmpty(variants[i].name);
			widths[i] = 0.98 / n;
		}
		table = Tables.createViewer(comp, headers);
		image = contributionImage(table).withFullWidth(25);
		table.setLabelProvider(this);
		Tables.bindColumnWidths(table, widths);
		Actions.bind(section, TableClipboard.onCopyAll(table));
		Actions.bind(table, TableClipboard.onCopySelected(table));

		selector.initWithEvent();
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof Cell[]))
			return null;
		var row = (Cell[]) obj;
		if (row.length <= col || row[col] == null)
			return null;
		var cell = row[col];
		return image.get(cell.share, cell.color);
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

	/**
	 * Creates for each variant a column of cells with the contribution values
	 * of that variant.
	 */
	private void updateCells(ToDoubleBiFunction<ContributionResult, TechFlow> fn) {
		var cells = new ArrayList<List<Cell>>();
		for (var variant : variants) {
			var map = new HashMap<CategorizedDescriptor, Double>();
			var result = this.result.getResult(variant);
			for (var techFlow : result.techIndex()) {
				map.compute(techFlow.process(), (process, value) -> {
					var v = fn.applyAsDouble(result, techFlow);
					return value != null
						? value + v
						: v;
				});
			}
			var column = map.entrySet()
				.stream()
				.map(e -> new Cell(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
			cells.add(column);
		}
		this.cells = cells;
	}

	private void updateRows() {
		var comparator = comparator();

		double absMax = 0;
		var rows = new ArrayList<Cell[]>();
		for (int i = 0; i < variants.length; i++) {

			// select the top contributions of variant i
			var column = cells.get(i);
			column.sort(comparator);
			var selected = new ArrayList<>(
				column.subList(0, Math.min(column.size(), count)));

			// calculate a rest if necessary
			if (column.size() > count) {
				var rest = column.subList(count, column.size())
					.stream()
					.mapToDouble(cell -> cell.result)
					.sum();
				if (rest != 0) {
					selected.add(Cell.restOf(rest));
				}
			}

			// fill column i in the rows with the respective contributions
			for (int j = 0; j < selected.size(); j++) {
				while (rows.size() <= j) {
					rows.add(new Cell[variants.length]);
				}
				var cell = selected.get(j);
				rows.get(j)[i] = cell;
				absMax = Math.max(absMax, Math.abs(cell.result));
			}
		}

		this.absMax = absMax;
		table.setInput(rows);

	}

	private Comparator<Cell> comparator() {
		if (Strings.nullOrEmpty(query))
			return Comparator.comparingDouble(Cell::result);

	}


	private static class Cell {

		final CategorizedDescriptor process;
		private final double result;

		double share;
		Color color;

		Cell(CategorizedDescriptor process, double result) {
			this.process = process;
			this.result = result;
		}

		static Cell of(Contribution<CategorizedDescriptor> contribution) {
			return new Cell(contribution.item, contribution.amount);
		}

		static Cell restOf(double result) {
			return new Cell(null, result);
		}

		boolean isRest() {
			return process == null;
		}

		double result() {
			return result;
		}
	}

	private static class RowBuilder {

		private final List<Cell[]> rows;
		private final int variantCount;
		private final TLongObjectHashMap<Color> colors;

		private RowBuilder(int variantCount) {
			this.variantCount = variantCount;
			this.rows = new ArrayList<>();
			this.colors = new TLongObjectHashMap<>();
		}

		void add(int variant, int row, Cell cell) {
			while (rows.size() <= row) {
				rows.add(new Cell[variantCount]);
			}
			if (cell.process == null) {
				cell.color = Colors.gray();
			} else {
				var color = colors.get(cell.process.id);
				if (color == null) {
					color = Colors.getForChart(colors.size() + 2);
					colors.put(cell.process.id, color);
				}
				cell.color = color;
			}
			var rowCells = rows.get(row);
			rowCells[variant] = cell;
		}

		List<Cell[]> get() {
			double absMax = 0;
			for (var row : rows) {
				for (var cell : row) {
					absMax = Math.max(absMax, Math.abs(cell.result));
				}
			}
			if (absMax == 0)
				return rows;
			for (var row : rows) {
				for (var cell : row) {
					cell.share = 0.1 + 0.9 * cell.result / absMax;
				}
			}
			return rows;
		}
	}
}
