package org.openlca.app.editors.epds;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Combo;
import org.openlca.app.util.Controls;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Unit;
import org.openlca.util.Strings;

class UnitCombo {

	private final Combo combo;
	private final ArrayList<Item> items = new ArrayList<>();
	private Consumer<Item> listener;

	private UnitCombo(Combo combo) {
		this.combo = combo;
		Controls.onSelect(combo, $ -> {
			int idx = combo.getSelectionIndex();
			if (idx < 0 || idx >= items.size())
				return;
			if (listener != null) {
				listener.accept(items.get(idx));
			}
		});
	}

	static UnitCombo of(Combo combo) {
		return new UnitCombo(combo);
	}

	void listen(Consumer<Item> listener) {
		this.listener = listener;
	}

	void fill(Flow flow) {
		if (flow == null) {
			clear();
			return;
		}

		items.clear();
		boolean isMultiProp = flow.flowPropertyFactors.size() > 1;
		for (var factor : flow.flowPropertyFactors) {
			var property = factor.flowProperty;
			if (property == null || property.unitGroup == null)
				continue;
			var group = property.unitGroup;
			for (var unit : group.units) {
				boolean isRef = Objects.equals(group.referenceUnit, unit)
					&& Objects.equals(flow.referenceFlowProperty, property);
				items.add(new Item(unit, factor, isRef));
			}
		}

		items.sort((i1, i2) -> isMultiProp
			? Strings.compare(i1.fullLabel(), i2.fullLabel())
			: Strings.compare(i1.shortLabel(), i2.shortLabel()));
		var comboItems = new String[items.size()];
		int selected = -1;
		for (int i = 0; i < items.size(); i++) {
			var item = items.get(i);
			if (item.isRef) {
				selected = i;
			}
			comboItems[i] = isMultiProp
				? item.fullLabel()
				: item.shortLabel();
		}

		combo.setItems(comboItems);
		if (selected >= 0) {
			combo.select(selected);
		}

	}

	void select(Unit unit, FlowProperty property) {
		int selected = -1;
		for (int i = 0; i < items.size(); i++) {
			var item = items.get(i);
			if (Objects.equals(unit, item.unit)
				&& Objects.equals(property, item.property())) {
				selected = i;
				break;
			}
		}
		if (selected >= 0) {
			combo.select(selected);
		}
	}

	void clear() {
		if (items.isEmpty())
			return;
		items.clear();
		combo.clearSelection();
		combo.setItems();
	}

	void pack() {
		combo.pack();
		var p = combo.getParent();
		var i = 0;
		while (i < 2 && p != null) {
			p.layout(true);
			i++;
			p = p.getParent();
		}
	}

	record Item(Unit unit, FlowPropertyFactor factor, boolean isRef) {

		String shortLabel() {
			return Strings.orEmpty(unit.name);
		}

		String fullLabel() {
			if (property() == null)
				return shortLabel();
			return shortLabel() + " - " + property().name;
		}

		FlowProperty property() {
			return factor.flowProperty;
		}
	}
}
