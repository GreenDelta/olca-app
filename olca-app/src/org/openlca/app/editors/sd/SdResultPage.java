package org.openlca.app.editors.sd;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.Var.Aux;
import org.openlca.sd.eqn.Var.Rate;
import org.openlca.sd.eqn.Var.Stock;

class SdResultPage extends FormPage {

	private final SdResultEditor editor;
	private Combo varCombo;
	private Var selectedVar;

	SdResultPage(SdResultEditor editor) {
		super(editor, "SdResultPage", "Results");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "Simulation results: " + editor.modelName());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		createVariableSelection(body, tk);
		createChartArea(body, tk);
	}

	private void createVariableSelection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Variable selection");
		UI.gridLayout(comp, 2);

		UI.label(comp, tk, "Variable:");
		varCombo = new Combo(comp, SWT.READ_ONLY);
		UI.fillHorizontal(varCombo);

		// Populate combo with variables
		var variables = editor.variables();
		if (variables != null) {
			var items = variables.stream()
					.map(this::getVariableLabel)
					.toArray(String[]::new);
			varCombo.setItems(items);

			if (!variables.isEmpty()) {
				varCombo.select(0);
				selectedVar = variables.get(0);
			}
		}

		Controls.onSelect(varCombo, e -> {
			int index = varCombo.getSelectionIndex();
			if (index >= 0 && index < variables.size()) {
				selectedVar = variables.get(index);
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
}
