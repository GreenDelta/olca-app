package org.openlca.app.editors.epds;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
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
			System.out.println("changed!");
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

	Optional<Item> fill(Flow flow) {
		if (flow == null) {
			clear();
			return Optional.empty();
		}

		items.clear();
		boolean isMultiProp = flow.flowPropertyFactors.size() > 1;
		for (var factor : flow.flowPropertyFactors) {
			if (factor.flowProperty == null
				|| factor.flowProperty.unitGroup == null)
				continue;
			var group = factor.flowProperty.unitGroup;
			for (var unit : group.units) {
				var item = new Item(unit, factor,
					Objects.equals(group.referenceUnit, unit));
				items.add(item);
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

		return selected >= 0
			? Optional.of(items.get(selected))
			: Optional.empty();
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
		items.clear();
		combo.clearSelection();
		combo.setItems();
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
