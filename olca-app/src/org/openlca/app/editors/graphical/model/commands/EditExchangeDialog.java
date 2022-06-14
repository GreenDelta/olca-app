package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Unit;
import org.openlca.util.Pair;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

class EditExchangeDialog extends FormDialog {

	private final Exchange exchange;
	private final List<Pair<FlowPropertyFactor, Unit>> units;

	private Text text;
	private Combo combo;

	/**
	 * Opens the dialog and returns true if the value of the
	 * exchange was changed.
	 */
	static boolean open(Exchange exchange) {
		if (exchange == null)
			return false;
		var dialog = new EditExchangeDialog(exchange);
		return dialog.open() == OK;
	}

	private EditExchangeDialog(Exchange exchange) {
		super(UI.shell());
		this.exchange = exchange;
		setBlockOnOpen(true);
		units = units(exchange);
	}


	private static List<Pair<FlowPropertyFactor, Unit>> units(Exchange e) {
		if (e == null || e.flow == null)
			return Collections.emptyList();
		var list = new ArrayList<Pair<FlowPropertyFactor, Unit>>();
		for (var f : e.flow.flowPropertyFactors) {
			if (f.flowProperty == null || f.flowProperty.unitGroup == null)
				continue;
			for (var unit : f.flowProperty.unitGroup.units) {
				list.add(Pair.of(f, unit));
			}
		}

		list.sort((p1, p2) -> {
			var prop1 = p1.first.flowProperty;
			var prop2 = p2.first.flowProperty;

			if (!Objects.equals(prop1, prop2)) {
				return Strings.compare(
						Labels.name(prop1),
						Labels.name(prop2));
			}

			var unit1 = p1.second;
			var unit2 = p2.second;
			return Strings.compare(
					Labels.name(unit1),
					Labels.name(unit2));
		});

		return list;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		var prefix = exchange.isInput
				? "Input of "
				: "Output of ";
		var title = prefix + Labels.name(exchange.flow);
		newShell.setText(title);
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 600, 350);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 2);
		text = UI.formText(body, tk, M.Amount);
		text.setText(Double.toString(exchange.amount));
		combo = UI.formCombo(body, tk, M.Unit);

		// fill the combo items

		// returns true if the given pair contains the reference unit
		Function<Pair<FlowPropertyFactor, Unit>, Boolean> isRef = pair -> {
			var prop = pair.first.flowProperty;
			var refProp = exchange.flow.referenceFlowProperty;
			if (!Objects.equals(prop, refProp))
				return false;
			var unit = pair.second;
			var refUnit = prop.unitGroup.referenceUnit;
			return Objects.equals(unit, refUnit);
		};

		var items = new String[units.size()];
		int selection = -1;
		for (int i = 0; i < units.size(); i++) {
			var pair = units.get(i);
			items[i] = Labels.name(pair.first.flowProperty)
					+ " - " + Labels.name(pair.second);

			// check if pair matches exchange unit
			if (Objects.equals(pair.first, exchange.flowPropertyFactor)
					&& Objects.equals(pair.second, exchange.unit)) {
				selection = i;
				continue;
			}

			// check if pair is reference unit
			if (selection < 0 && isRef.apply(pair)) {
				selection = i;
			}
		}

		combo.setItems(items);
		if (selection >= 0) {
			combo.select(selection);
		}
	}

	@Override
	protected void okPressed() {
		int i = combo.getSelectionIndex();
		if (units.isEmpty() || i < 0) {
			MsgBox.error("No unit selected");
			return;
		}
		var unit = units.get(i);

		try {
			exchange.amount = Double.parseDouble(text.getText());
		} catch (Exception e) {
			MsgBox.error(text.getText() + " is not a number.");
			return;
		}

		exchange.flowPropertyFactor = unit.first;
		exchange.unit = unit.second;
		super.okPressed();
	}

}
