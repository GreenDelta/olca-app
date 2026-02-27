package org.openlca.app.editors.sd.results;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.sd.interop.CoupledResult;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.UI;
import org.openlca.sd.model.Var;
import org.openlca.sd.model.Var.Aux;
import org.openlca.sd.model.Var.Rate;
import org.openlca.sd.model.Var.Stock;

class SdResultPage extends FormPage {

	private final SdResultEditor editor;
	private final List<Var> vars;
	private final CoupledResult result;

	SdResultPage(SdResultEditor editor) {
		super(editor, "SdResultPage", "Results");
		this.editor = editor;
		this.vars = editor.vars();
		this.result = editor.result();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "Simulation results: " + editor.modelName());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		infoSection(body, tk);
		variableChartSection(body, tk);
		if (result != null && result.hasImpactResults()) {
			impactChartSection(body, tk);
		}
	}

	private void infoSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		UI.gridLayout(comp, 2);

		var nameText = UI.labeledText(comp, tk, "Model");
		nameText.setEditable(false);
		nameText.setText(editor.modelName());

		var varsCount = VarsCount.of(vars);
		var varsText = UI.labeledText(comp, tk, "Variables");
		varsText.setEditable(false);
		varsText.setText(varsCount.toString());

		if (result != null && result.hasImpactResults()) {
			var impactsText = UI.labeledText(comp, tk, "Impact categories");
			impactsText.setEditable(false);
			impactsText.setText(Integer.toString(result.getImpactCategories().size()));
		}

		var iterText = UI.labeledText(comp, tk, "Iterations");
		iterText.setEditable(false);
		iterText.setText(Integer.toString(varsCount.iterations));

		UI.filler(comp, tk);
		var exportBtn = UI.button(comp, tk, "Export results...");
		exportBtn.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(exportBtn, e -> {
			var file = FileChooser.forSavingFile("Export simulation results",
				editor.modelName() + "_results.xlsx");
			if (file == null)
				return;
			var res = App.exec("Export results...", () -> {
				return XlsExport.run(result, file);
			});
			if (res.isError()) {
				ErrorReporter.on("Failed to export simulation results", res.error());
			}
		});
	}

	private void variableChartSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Variables");
		UI.gridLayout(comp, 1);

		var comboComp = UI.composite(comp, tk);
		UI.gridLayout(comboComp, 2);
		UI.gridData(comboComp, true, false);

		UI.label(comboComp, tk, "Variable");
		var combo = new Combo(comboComp, SWT.READ_ONLY);
		UI.fillHorizontal(combo);

		var items = vars.stream()
			.map(v -> v.name().label())
			.toArray(String[]::new);
		combo.setItems(items);

		var chart = new ResultChart(comp, result);
		if (!vars.isEmpty()) {
			combo.select(0);
			chart.show(vars.getFirst());
		}

		Controls.onSelect(combo, e -> {
			int i = combo.getSelectionIndex();
			if (i >= 0 && i < vars.size()) {
				chart.show(vars.get(i));
			}
		});
	}

	private void impactChartSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Impact categories");
		UI.gridLayout(comp, 1);

		var impacts = result.getImpactCategories();
		var comboComp = UI.composite(comp, tk);
		UI.gridLayout(comboComp, 2);
		UI.gridData(comboComp, true, false);

		UI.label(comboComp, tk, "Impact category");
		var combo = new Combo(comboComp, SWT.READ_ONLY);
		UI.fillHorizontal(combo);

		var items = impacts.stream()
			.map(i -> i.name)
			.toArray(String[]::new);
		combo.setItems(items);

		var chart = new ResultChart(comp, result);
		if (!impacts.isEmpty()) {
			combo.select(0);
			chart.show(impacts.getFirst());
		}

		Controls.onSelect(combo, e -> {
			int idx = combo.getSelectionIndex();
			if (idx >= 0 && idx < impacts.size()) {
				chart.show(impacts.get(idx));
			}
		});
	}

	private record VarsCount(
		int stocks, int rates, int auxs, int iterations) {
		static VarsCount of(Iterable<Var> vars) {
			int stocks = 0;
			int rates = 0;
			int auxs = 0;
			int iterations = 0;
			if (vars != null) {
				for (var v : vars) {
					switch (v) {
						case Stock ignored -> stocks++;
						case Rate ignored -> rates++;
						case Aux ignored -> auxs++;
					}
					iterations = Math.max(iterations, v.values().size());
				}
			}
			return new VarsCount(stocks, rates, auxs, iterations);
		}

		@Override
		public String toString() {
			return "Stocks: " + stocks + ", Rates: " + rates + ", Aux: " + auxs;
		}
	}
}
