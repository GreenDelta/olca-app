package org.openlca.app.editors.projects.results;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.components.ResultItemSelector;
import org.openlca.app.results.Sort;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
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
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.ProjectResult;
import org.openlca.core.results.ResultItemView;
import org.openlca.util.Strings;

import gnu.trove.map.hash.TLongObjectHashMap;

class ContributionSection extends LabelProvider implements TableSection,
	ResultItemSelector.SelectionHandler {

	private final ProjectResult result;
	private final ProjectVariant[] variants;
	private TableViewer table;
	private ContributionImage image;
	private final TLongObjectHashMap<Color> colors;

	private String unit;
	private int count = 10;
	private String query;
	private double absMax;
	private List<List<Cell>> cells;

	private ContributionSection(ProjectResult result) {
		this.result = result;
		this.variants = variantsOf(result);
		this.colors = new TLongObjectHashMap<>();
	}

	static ContributionSection of(ProjectResult result) {
		return new ContributionSection(result);
	}

	@Override
	public void renderOn(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Result contributions");
		var comp = UI.sectionClient(section, tk, 1);
		UI.gridLayout(comp, 1);

		// create the result selector
		var selectorComp = tk.createComposite(comp);
		UI.gridLayout(selectorComp, 2, 5, 0);
		var items = ResultItemView.of(result);
		Sort.sort(items);
		var selector = ResultItemSelector.on(items)
			.withSelectionHandler(this)
			.create(selectorComp, tk);

		// add the search text and count selector
		var queryComp = tk.createComposite(comp);
		UI.gridData(queryComp, true, false);
		UI.gridLayout(queryComp, 2, 5, 0);
		var searchText = tk.createText(queryComp, "");
		searchText.setMessage("Search a process ...");
		UI.gridData(searchText, true, false);
		searchText.addModifyListener($ -> {
			query = searchText.getText();
			updateRows();
		});

		var spinner = new Spinner(queryComp, SWT.BORDER);
		spinner.setIncrement(1);
		spinner.setMinimum(1);
		spinner.setSelection(count);
		tk.adapt(spinner);
		Controls.onSelect(spinner, $ -> {
			count = Math.max(1, spinner.getSelection());
			updateRows();
		});

		// create the table
		var headers = new String[variants.length];
		var widths = new double[variants.length];
		var n = variants.length == 0 ? 1 : variants.length;
		for (int i = 0; i < variants.length; i++) {
			headers[i] = Strings.orEmpty(variants[i].name);
			widths[i] = 0.98 / n;
		}
		table = Tables.createViewer(comp, headers);
		image = contributionImage(table).withFullWidth(40);
		table.setLabelProvider(this);
		Tables.bindColumnWidths(table, widths);
		Actions.bind(section, TableClipboard.onCopyAll(table));
		Actions.bind(table, TableClipboard.onCopySelected(table));

		// fire the initial selection
		selector.initWithEvent();
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof Cell[]) || absMax == 0)
			return null;
		var row = (Cell[]) obj;
		if (row.length <= col || row[col] == null)
			return null;
		var cell = row[col];
		double share = 0.1 + 0.9 * cell.result / absMax;
		Color color;
		if (cell.isRest) {
			color = Colors.gray();
		} else {
			color = colors.get(cell.process.id);
			if (color == null) {
				// +2 to avoid red and blue currently as these
				// look very similar to the contribution colors
				color = Colors.getForChart(colors.size() + 2);
				colors.put(cell.process.id, color);
			}
		}
		return image.get(share, color);
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
		return result + " | " + (cell.isRest
			? "Others"
			: Labels.name(cell.process));
	}

	@Override
	public void onFlowSelected(EnviFlow flow) {
		unit = Labels.refUnit(flow);
		updateCells((result, techFlow)
			-> result.getDirectFlowResult(techFlow, flow));
	}

	@Override
	public void onImpactSelected(ImpactDescriptor impact) {
		unit = impact.referenceUnit;
		updateCells((result, techFlow)
			-> result.getDirectImpactResult(techFlow, impact));
	}

	@Override
	public void onCostsSelected(CostResultDescriptor cost) {
		unit = Labels.getReferenceCurrencyCode();
		updateCells((result, techFlow) -> cost.forAddedValue
			? -result.getDirectCostResult(techFlow)
			: result.getDirectCostResult(techFlow));
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
		updateRows();
	}

	private void updateRows() {
		if (cells == null)
			return;

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
		this.colors.clear();
		table.setInput(rows);

	}

	private Comparator<Cell> comparator() {
		// compare by result values by default
		if (Strings.nullOrEmpty(query))
			return Comparator.comparingDouble(cell -> -cell.result);

		// compare by a match factor if there is a search query
		var terms = Arrays.stream(query.split(" "))
			.map(s -> s.trim().toLowerCase())
			.filter(t -> !Strings.nullOrEmpty(t))
			.collect(Collectors.toSet());

		// the smaller the match value the higher a cell will
		// be ranked. 0 means no match so that we can give 1
		// to the rest so that it is always at the bottom
		ToDoubleFunction<String> matcher = s -> {
			if (s == null)
				return 0;
			var f = s.toLowerCase();
			double i = 0;
			for (var term : terms) {
				double idx = f.indexOf(term);
				if (idx >= 0) {
					i -= term.length() / (f.length() - term.length() + idx + 1.0);
				}
			}
			return i;
		};

		return Comparator.comparingDouble(
			cell -> cell.isRest
				? 1
				: matcher.applyAsDouble(Labels.name(cell.process)));
	}


	private static class Cell {

		private final CategorizedDescriptor process;
		private final double result;
		private final boolean isRest;

		Cell(CategorizedDescriptor process, double result) {
			this.process = process;
			this.result = result;
			this.isRest = process == null;
		}

		static Cell restOf(double result) {
			return new Cell(null, result);
		}
	}
}
