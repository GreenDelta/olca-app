package org.openlca.app.editors.processes.allocation;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.model.AllocationMethod;
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

	static FactorCalculation of(Process p) {
		var calc = FactorCalculation.of(p);
		if (p == null)
			return calc;
		var props = Factors.allocationPropertiesOf(p);
		if (props.isEmpty()) {
			MsgBox.error("No common allocation properties",
				"There is no common flow property of the product" +
					" outputs and waste inputs that could be used" +
					" to calculate allocation factors.");
			return calc;
		}
		var dialog = new CalculationDialog(props);
		if (dialog.open() != Window.OK)
			return calc;
		calc.bind(AllocationMethod.PHYSICAL, dialog.physical.selected());
		calc.bind(AllocationMethod.ECONOMIC, dialog.economic.selected());
		calc.bind(AllocationMethod.CAUSAL, dialog.causal.selected());
		return calc;
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

	private record PropCombo(
		Combo combo, AtomicReference<FactorCalculation.Ref> selection) {

		static PropCombo create(Composite comp, FormToolkit tk, String title) {
			var combo = UI.formCombo(comp, tk, title);
			UI.fillHorizontal(combo);
			return new PropCombo(combo, new AtomicReference<>());
		}

		PropCombo fill(Set<FlowProperty> set, FlowPropertyType prefType) {

			// sort properties into a list
			var props = new ArrayList<>(set);
			props.sort((p1, p2) -> {
				var t1 = Objects.equals(p1.flowPropertyType, prefType);
				var t2 = Objects.equals(p2.flowPropertyType, prefType);
				return t1 != t2
					? t1 ? -1 : 1
					: Strings.compare(p1.name, p2.name);
			});

			// fill the combo box
			var items = props
				.stream()
				.map(p -> p.name)
				.toArray(String[]::new);
			combo.setItems(items);
			combo.select(0);
			selection.set(FactorCalculation.Ref.of(props.get(0)));

			// handle selection changes
			Controls.onSelect(combo, $ -> {
				var idx = combo.getSelectionIndex();
				var prop = props.get(idx);
				selection.set(FactorCalculation.Ref.of(prop));
			});

			return this;
		}

		FactorCalculation.Ref selected() {
			return selection.get();
		}
	}
}
