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
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.UI;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.Var.Aux;
import org.openlca.sd.eqn.Var.Rate;
import org.openlca.sd.eqn.Var.Stock;

class SdResultPage extends FormPage {

	private final SdResultEditor editor;
	private final List<Var> vars;

	SdResultPage(SdResultEditor editor) {
		super(editor, "SdResultPage", "Results");
		this.editor = editor;
		this.vars = editor.vars();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "Simulation results: " + editor.modelName());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		infoSection(body, tk);
		chartSection(body, tk);
	}

	private void infoSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		UI.gridLayout(comp, 2);

		var nameText = UI.labeledText(comp, tk, "Model");
		nameText.setEditable(false);
		nameText.setText(editor.modelName());

		var varsCount = VarsCount.of(vars);
		var varsText = UI.labeledText(comp, tk, "Number of variables");
		varsText.setEditable(false);
		varsText.setText(varsCount.toString());

		var iterText = UI.labeledText(comp, tk, "Iterations");
		iterText.setEditable(false);
		iterText.setText(Integer.toString(varsCount.iterations));

		UI.filler(comp, tk);
		var exportBtn = UI.button(comp, tk, "Export results...");
		exportBtn.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(exportBtn, e -> {
			var file = FileChooser.forSavingFile("Export simulation results",
					editor.modelName() + "_results.xlsx");
			var res = App.exec(
					"Export results...", () -> SdResultExport.run(vars, file));
			if (res.hasError()) {
				ErrorReporter.on("Failed to export simulation results", res.error());
			}
		});
	}

	private void chartSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Numeric variables");
		UI.gridLayout(comp, 1);

		var numVars = Util.numericVarsOf(vars);

		// Variable selection row
		var selectionComp = UI.composite(comp, tk);
		UI.gridLayout(selectionComp, 2);
		UI.gridData(selectionComp, true, false);

		UI.label(selectionComp, tk, "Variable");
		var combo = new Combo(selectionComp, SWT.READ_ONLY);
		UI.fillHorizontal(combo);

		var items = numVars.stream()
				.map(v -> v.name().label())
				.toArray(String[]::new);
		combo.setItems(items);

		var chart = new SdResultChart(comp, 300);
		if (!numVars.isEmpty()) {
			combo.select(0);
			var firstVar = numVars.getFirst();
			chart.update(firstVar);
		}

		// Handle variable selection changes
		Controls.onSelect(combo, e -> {
			int idx = combo.getSelectionIndex();
			if (idx >= 0 && idx < numVars.size()) {
				var selectedVar = numVars.get(idx);
				chart.update(selectedVar);
			}
		});
	}

	private record VarsCount(
			int stocks, int rates, int auxs, int iterations
	) {
		static VarsCount of(Iterable<Var> vars) {
			int stocks = 0, rates = 0, auxs = 0, iterations = 0;
			if (vars != null) {
				for (var v : vars) {
					if (v instanceof Stock) {
						stocks++;
					} else if (v instanceof Rate) {
						rates++;
					} else if (v instanceof Aux) {
						auxs++;
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
