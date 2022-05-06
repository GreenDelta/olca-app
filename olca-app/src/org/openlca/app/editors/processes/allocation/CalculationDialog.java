package org.openlca.app.editors.processes.allocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.Process;
import org.openlca.util.Strings;

/**
 * A dialog for calculating default allocation factors.
 */
class CalculationDialog extends FormDialog {

	private final Set<FlowProperty> props;
	private PropCombo physical;
	private PropCombo economic;
	private PropCombo causal;

	static void show(Process p) {
		if (p == null)
			return;
		var props = FactorCalculator.allocationPropertiesOf(p);
		if (props.isEmpty()) {
			MsgBox.error("No common allocation properties",
				"There is no common flow property of the product" +
					" outputs and waste inputs that could be used" +
					" to calculate allocation factors.");
			return;
		}
		new CalculationDialog(props).open();
	}

	private CalculationDialog(Set<FlowProperty> props) {
		super(UI.shell());
		this.props = props;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Calculate default factors");
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		tk.createLabel(body,
			"Select the flow properties that should be used for" +
				" calculating the respective allocation factors.");
		var comp = UI.formComposite(body, tk);
		UI.fillHorizontal(comp);

		physical = PropCombo.create(comp, tk, "Physical allocation:")
			.fill(props, FlowPropertyType.PHYSICAL);
		economic = PropCombo.create(comp, tk, "Economic allocation:")
			.fill(props, FlowPropertyType.ECONOMIC);
		causal = PropCombo.create(comp, tk, "Causal allocation:")
			.fill(props, FlowPropertyType.PHYSICAL);
	}

	private record PropCombo(Combo combo, List<FlowProperty> props) {

		static PropCombo create(Composite comp, FormToolkit tk, String title) {
			var combo = UI.formCombo(comp, tk, title);
			UI.fillHorizontal(combo);
			return new PropCombo(combo, new ArrayList<>());
		}

		PropCombo fill(Set<FlowProperty> common, FlowPropertyType prefType) {
			props.addAll(common);
			props.sort((p1, p2) -> {
				var t1 = Objects.equals(p1.flowPropertyType, prefType);
				var t2 = Objects.equals(p2.flowPropertyType, prefType);
				return t1 != t2
					? t1 ? -1 : 1
					: Strings.compare(p1.name, p2.name);
			});
			var items = props
				.stream()
				.map(p -> p.name)
				.toArray(String[]::new);
			combo.setItems(items);
			combo.select(0);
			return this;
		}

		FlowProperty selected() {
			return props.get(combo.getSelectionIndex());
		}
	}

}
