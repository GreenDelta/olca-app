package org.openlca.app.editors.sd;

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

	private Combo varCombo;
	private Var selectedVar;

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
		createVariableSelection(body, tk);
		createChartArea(body, tk);
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

	private void createVariableSelection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Variable selection");
		UI.gridLayout(comp, 2);

		UI.label(comp, tk, "Variable:");
		varCombo = new Combo(comp, SWT.READ_ONLY);
		UI.fillHorizontal(varCombo);

		// Populate combo with variables

		var items = vars.stream()
				.map(this::getVariableLabel)
				.toArray(String[]::new);
		varCombo.setItems(items);

		if (!vars.isEmpty()) {
			varCombo.select(0);
			selectedVar = vars.getFirst();
		}

		Controls.onSelect(varCombo, e -> {
			int index = varCombo.getSelectionIndex();
			if (index >= 0 && index < vars.size()) {
				selectedVar = vars.get(index);
				updateChart();
			}
		});
	}

	private void createChartArea(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Chart");
		UI.gridLayout(comp, 1);

		// Placeholder for chart - will be added later
		var placeholder = UI.label(comp, tk, "Chart will be displayed here");
	}

	private String getVariableLabel(Var var) {
		if (var == null)
			return "Unknown";
		var type = getVariableType(var);
		var name = var.name() != null ? var.name().label() : "Unknown";
		return type + ": " + name;
	}

	private String getVariableType(Var var) {
		return switch (var) {
			case Stock ignored -> "Stock";
			case Aux ignored -> "Aux";
			case Rate ignored -> "Rate";
			case null -> "None";
		};
	}

	private void updateChart() {
		// TODO: Update chart when variable selection changes
		// This will be implemented when the chart is added
	}

	public Var getSelectedVariable() {
		return selectedVar;
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
