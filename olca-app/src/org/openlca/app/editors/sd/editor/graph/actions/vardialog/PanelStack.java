package org.openlca.app.editors.sd.editor.graph.actions.vardialog;

import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.sd.model.cells.Cell;
import org.openlca.sd.model.cells.LookupCell;
import org.openlca.sd.model.cells.LookupEqnCell;
import org.openlca.sd.model.cells.NonNegativeCell;
import org.openlca.sd.model.cells.TensorCell;
import org.openlca.sd.model.cells.TensorEqnCell;

import java.util.List;

class PanelStack {

	private final Combo combo;
	private final Composite stack;
	private final StackLayout stackLayout;

	private final EquationPanel equationPanel;
	private final LookupPanel lookupPanel;
	private final TensorPanel tensorPanel;

	private Panel top;
	private Cell input;
	private ChangeObserver onChange;

	PanelStack(Composite comp, FormToolkit tk, boolean stockVar) {

		combo = UI.labeledCombo(comp, tk, "Type");
		combo.setItems(PanelType.items());
		combo.select(0);
		Controls.onSelect(combo, $ -> {
			var type = PanelType.values()[combo.getSelectionIndex()];
			updatePanel(type);
		});

		UI.filler(comp, tk);
		stack = UI.composite(comp, tk);
		UI.gridData(stack, true, true);
		stackLayout = new StackLayout();
		stack.setLayout(stackLayout);

		equationPanel = new EquationPanel(stack, tk);
		lookupPanel = new LookupPanel(stack, tk, stockVar);
		tensorPanel = new TensorPanel(stack, tk, stockVar);
		List.of(equationPanel, lookupPanel, tensorPanel)
			.forEach(p -> p.onChange(b -> {
				if (onChange != null) {
					onChange.reactOn(b);
				}
			}));

		top = equationPanel;
	}

	void setInput(Cell input) {
		this.input = input;
		var type = PanelType.of(input);
		combo.select(type.ordinal());
		updatePanel(type);
	}

	void onChange(ChangeObserver onChange) {
		this.onChange = onChange;
	}

	Cell getCell() {
		return top.getCell();
	}

	private void updatePanel(PanelType type) {
		top = switch (type) {
			case LOOKUP -> lookupPanel;
			case TENSOR -> tensorPanel;
			default -> equationPanel;
		};
		stackLayout.topControl = top.composite();
		stack.layout(true, true);
		if (input != null) {
			top.setInput(input);
		}
	}

	private enum PanelType {

		EQUATION,
		LOOKUP,
		TENSOR;

		static PanelType of(Cell cell) {
			return switch (cell) {
				case NonNegativeCell(Cell inner) -> of(inner);
				case LookupCell ignored -> LOOKUP;
				case LookupEqnCell ignored -> LOOKUP;
				case TensorCell ignored -> TENSOR;
				case TensorEqnCell ignored -> TENSOR;
				case null, default -> EQUATION;
			};
		}

		static String[] items() {
			var types = values();
			var items = new String[types.length];
			for (int i = 0; i < types.length; i++) {
				items[i] = types[i].toString();
			}
			return items;
		}

		@Override
		public String toString() {
			return switch (this) {
				case EQUATION -> "Equation";
				case LOOKUP -> "Lookup function";
				case TENSOR -> "Array";
			};
		}
	}
}
