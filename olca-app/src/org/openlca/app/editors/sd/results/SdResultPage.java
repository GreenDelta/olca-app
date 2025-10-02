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
import org.openlca.app.editors.sd.interop.CoupledResult;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.Var.Aux;
import org.openlca.sd.eqn.Var.Rate;
import org.openlca.sd.eqn.Var.Stock;

class SdResultPage extends FormPage {

	private final SdResultEditor editor;
	private final List<Var> vars;
	private final CoupledResult coupledResult;

	SdResultPage(SdResultEditor editor) {
		super(editor, "SdResultPage", "Results");
		this.editor = editor;
		this.vars = editor.vars();
		this.coupledResult = editor.result();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "Simulation results: " + editor.modelName());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		infoSection(body, tk);
		variableChartSection(body, tk);
		if (coupledResult != null && coupledResult.hasImpactResults()) {
			impactChartSection(body, tk);
		}
	}

	private void infoSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		UI.gridLayout(comp, 2);

		var nameText = UI.labeledText(comp, tk, "Model");
		nameText.setEditable(false);
		nameText.setText(editor.modelName());

		var varsCount = VarsCount.of(vars, coupledResult);
		var varsText = UI.labeledText(comp, tk, "Variables");
		varsText.setEditable(false);
		varsText.setText(varsCount.toString());

		if (coupledResult != null && coupledResult.hasImpactResults()) {
			var impactsText = UI.labeledText(comp, tk, "Impact categories");
			impactsText.setEditable(false);
			impactsText.setText(Integer.toString(coupledResult.getImpactCategories().size()));
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
			var res = App.exec("Export results...", () -> {
					return SdResultExport.run(vars, file);
			});
			if (res.hasError()) {
				ErrorReporter.on("Failed to export simulation results", res.error());
			}
		});
	}

	private void variableChartSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Variables");
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
			updateVariableChart(chart, firstVar);
		}

		// Handle variable selection changes
		Controls.onSelect(combo, e -> {
			int idx = combo.getSelectionIndex();
			if (idx >= 0 && idx < numVars.size()) {
				var selectedVar = numVars.get(idx);
				updateVariableChart(chart, selectedVar);
			}
		});
	}

	private void updateVariableChart(SdResultChart chart, Var var) {
		if (coupledResult != null) {
			var values = coupledResult.varResultsOf(var);
			chart.update(var.name().label(), values);
		} else {
			chart.update(var);
		}
	}

	private void impactChartSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Impact categories");
		UI.gridLayout(comp, 1);

		var impacts = coupledResult.getImpactCategories();

		// Impact selection row
		var selectionComp = UI.composite(comp, tk);
		UI.gridLayout(selectionComp, 2);
		UI.gridData(selectionComp, true, false);

		UI.label(selectionComp, tk, "Impact category");
		var combo = new Combo(selectionComp, SWT.READ_ONLY);
		UI.fillHorizontal(combo);

		var items = impacts.stream()
				.map(i -> i.name)
				.toArray(String[]::new);
		combo.setItems(items);

		var chart = new SdResultChart(comp, 300);
		if (!impacts.isEmpty()) {
			combo.select(0);
			var firstImpact = impacts.getFirst();
			updateImpactChart(chart, firstImpact);
		}

		// Handle impact selection changes
		Controls.onSelect(combo, e -> {
			int idx = combo.getSelectionIndex();
			if (idx >= 0 && idx < impacts.size()) {
				var selectedImpact = impacts.get(idx);
				updateImpactChart(chart, selectedImpact);
			}
		});
	}

	private void updateImpactChart(SdResultChart chart, ImpactDescriptor impact) {
		var values = coupledResult.impactResultsOf(impact);
		var unit = impact.referenceUnit != null ? impact.referenceUnit : "";
		var name = impact.name + (unit.isEmpty() ? "" : " [" + unit + "]");
		chart.update(name, values);
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

		static VarsCount of(Iterable<Var> vars, CoupledResult result) {
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
					if (result != null) {
						iterations = Math.max(iterations, result.varResultsOf(v).length);
					} else {
						iterations = Math.max(iterations, v.values().size());
					}
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
