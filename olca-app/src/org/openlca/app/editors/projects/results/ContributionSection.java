package org.openlca.app.editors.projects.results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.ToDoubleBiFunction;
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
import org.openlca.app.editors.projects.ProjectResultData;
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
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.results.ContributionResult;
import org.openlca.util.Strings;

import gnu.trove.map.hash.TLongObjectHashMap;

class ContributionSection extends LabelProvider implements TableSection,
	ResultItemSelector.SelectionHandler {

	private final ProjectResultData data;
	private final ProjectVariant[] variants;
	private TableViewer table;
	private ContributionImage image;
	private final TLongObjectHashMap<Color> colors;

	private String unit;
	private int count = 10;
	private String query;
	private double absMax;
	private List<List<Contribution>> cells;

	private ContributionSection(ProjectResultData data) {
		this.data = data;
		this.variants = data.variants();
		this.colors = new TLongObjectHashMap<>();
	}

	static ContributionSection of(ProjectResultData data) {
		return new ContributionSection(data);
	}

	@Override
	public void renderOn(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Result contributions");
		var comp = UI.sectionClient(section, tk, 1);
		UI.gridLayout(comp, 1);

		var configComp = tk.createComposite(comp);
		UI.gridLayout(configComp, 2, 5, 0);

		// create the result selector
		var selectorComp = tk.createComposite(configComp);
		UI.gridLayout(selectorComp, 2, 5, 0);
		var selector = ResultItemSelector.on(data.items())
			.withSelectionHandler(this)
			.create(selectorComp, tk);
		UI.filler(configComp, tk);

		// add the search text and count selector
		var searchText = tk.createText(configComp, "");
		searchText.setMessage("Search a process ...");
		UI.gridData(searchText, true, false);
		searchText.addModifyListener($ -> {
			query = searchText.getText();
			updateRows();
		});

		var spinner = new Spinner(configComp, SWT.BORDER);
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
		if (!(obj instanceof Contribution[]) || absMax == 0)
			return null;
		var row = (Contribution[]) obj;
		if (row.length <= col || row[col] == null)
			return null;
		var cell = row[col];
		double share = 0.1 + 0.9 * cell.amount / absMax;
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
		if (!(obj instanceof Contribution[]))
			return null;
		var row = (Contribution[]) obj;
		if (row.length <= col || row[col] == null)
			return null;
		var cell = row[col];
		var result = Numbers.format(cell.amount, 2);
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
		var cells = new ArrayList<List<Contribution>>();
		for (var variant : variants) {
			var map = new HashMap<RootDescriptor, Double>();
			var result = data.result().getResult(variant);
			for (var techFlow : result.techIndex()) {
				map.compute(techFlow.provider(), (process, value) -> {
					var v = fn.applyAsDouble(result, techFlow);
					return value != null
						? value + v
						: v;
				});
			}
			var column = map.entrySet()
				.stream()
				.map(e -> new Contribution(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
			cells.add(column);
		}
		this.cells = cells;
		updateRows();
	}

	private void updateRows() {
		if (cells == null)
			return;

		double absMax = 0;
		var rows = new ArrayList<Contribution[]>();
		for (int i = 0; i < variants.length; i++) {
			// select the top contributions of variant i
			var selected = Contribution.select(cells.get(i), count, query);

			// fill column i in the rows with the respective contributions
			for (int j = 0; j < selected.size(); j++) {
				while (rows.size() <= j) {
					rows.add(new Contribution[variants.length]);
				}
				var cell = selected.get(j);
				rows.get(j)[i] = cell;
				absMax = Math.max(absMax, Math.abs(cell.amount));
			}
		}

		this.absMax = absMax;
		this.colors.clear();
		table.setInput(rows);

	}
}
