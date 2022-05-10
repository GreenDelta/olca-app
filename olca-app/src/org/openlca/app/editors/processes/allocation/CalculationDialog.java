package org.openlca.app.editors.processes.allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
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
import org.openlca.util.AllocationRef;
import org.openlca.util.AllocationUtils;
import org.openlca.util.Strings;

/**
 * A dialog for calculating default allocation factors.
 */
class CalculationDialog extends FormDialog {

	private final Set<FlowProperty> props;
	private Selector physical;
	private Selector economic;
	private Selector causal;

	static List<AllocationRef> of(Process p) {
		var products = AllocationUtils.getProviderFlows(p);
		if (products.size() < 2) {
			MsgBox.error("Not a multi-functional process",
				"Allocation factors cannot be calculated because " +
					"this is not a process with multiple output " +
					"products or waste inputs.");
			return Collections.emptyList();
		}
		var props = AllocationUtils.allocationPropertiesOf(p);
		if (props.isEmpty()) {
			MsgBox.error("No common allocation properties",
				"There is no common flow property of the product" +
					" outputs and waste inputs that could be used" +
					" to calculate allocation factors.");
			return Collections.emptyList();
		}

		var dialog = new CalculationDialog(props);
		return dialog.open() != Window.OK
			? Collections.emptyList()
			: Stream.of(
				dialog.physical.selected(),
				dialog.economic.selected(),
				dialog.causal.selected())
			.filter(Objects::nonNull)
			.toList();
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

		Function<AllocationMethod, Selector> selector =
			method -> Selector.of(method, props).render(comp, tk);
		physical = selector.apply(AllocationMethod.PHYSICAL);
		economic = selector.apply(AllocationMethod.ECONOMIC);
		causal = selector.apply(AllocationMethod.CAUSAL);
	}

	private record Selector(
		AllocationMethod method,
		List<FlowProperty> props,
		AtomicReference<AllocationRef> selection
	) {

		static Selector of(AllocationMethod method, Set<FlowProperty> set) {
			var prefType = method == AllocationMethod.ECONOMIC
				? FlowPropertyType.ECONOMIC
				: FlowPropertyType.PHYSICAL;
			var props = new ArrayList<>(set);
			props.sort((p1, p2) -> {
				var t1 = Objects.equals(p1.flowPropertyType, prefType);
				var t2 = Objects.equals(p2.flowPropertyType, prefType);
				return t1 != t2
					? t1 ? -1 : 1
					: Strings.compare(p1.name, p2.name);
			});
			var initial = AllocationRef.of(method, props.get(0));
			return new Selector(method, props, new AtomicReference<>(initial));
		}

		Selector render(Composite comp, FormToolkit tk) {
			var title = switch (method) {
				case PHYSICAL -> "Physical allocation:";
				case ECONOMIC -> "Economic allocation:";
				default -> "Causal allocation:";
			};

			var combo = UI.formCombo(comp, tk, title);
			UI.fillHorizontal(combo);
			var items = props
				.stream()
				.map(p -> p.name)
				.toArray(String[]::new);
			combo.setItems(items);
			combo.select(0);
			Controls.onSelect(combo, $ -> {
				var idx = combo.getSelectionIndex();
				var prop = props.get(idx);
				selection.set(AllocationRef.of(method, prop));
			});

			if (method == AllocationMethod.ECONOMIC) {
				UI.filler(comp);
				var check = tk.createButton(
					comp, "Calculate from costs/revenues", SWT.CHECK);
				Controls.onSelect(check, $ -> {
					if (check.getSelection()) {
						combo.setEnabled(false);
						selection.set(AllocationRef.ofCosts(method));
					} else {
						combo.setEnabled(true);
						var idx = combo.getSelectionIndex();
						var prop = props.get(idx);
						selection.set(AllocationRef.of(method, prop));
					}
				});
			}

			return this;
		}


		AllocationRef selected() {
			return selection.get();
		}
	}
}
