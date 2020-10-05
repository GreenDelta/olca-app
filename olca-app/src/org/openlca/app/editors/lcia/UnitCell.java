package org.openlca.app.editors.lcia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openlca.app.editors.lcia.UnitCell.UnitItem;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.util.Strings;

/**
 * A cell editor for setting units and flow properties of an LCIA factor.
 */
class UnitCell extends ComboBoxCellModifier<ImpactFactor, UnitItem> {

	private final ImpactCategoryEditor editor;

	UnitCell(ImpactCategoryEditor editor) {
		this.editor = editor;
	}

	@Override
	protected UnitItem[] getItems(ImpactFactor factor) {
		if (factor == null || factor.flow == null)
			return new UnitItem[0];
		Flow flow = factor.flow;
		List<UnitItem> items = new ArrayList<>();
		for (FlowPropertyFactor prop : flow.flowPropertyFactors) {
			if (prop.flowProperty == null)
				continue;
			UnitGroup group = prop.flowProperty.unitGroup;
			if (group == null)
				continue;
			for (Unit unit : group.units) {
				items.add(new UnitItem(factor, prop, unit));
			}
		}
		Collections.sort(items);
		return items.toArray(new UnitItem[0]);
	}

	@Override
	protected UnitItem getItem(ImpactFactor factor) {
		if (factor == null)
			return null;
		return new UnitItem(factor,
				factor.flowPropertyFactor, factor.unit);
	}

	@Override
	protected String getText(UnitItem item) {
		if (item == null)
			return "";
		return item.toString();
	}

	@Override
	protected void setItem(ImpactFactor factor, UnitItem item) {
		if (factor == null || item == null)
			return;
		if (Objects.equals(factor.unit, item.unit)
				&& Objects.equals(factor.flowPropertyFactor, item.property))
			return;
		factor.unit = item.unit;
		factor.flowPropertyFactor = item.property;
		editor.setDirty(true);
	}

	static class UnitItem implements Comparable<UnitItem> {

		final ImpactFactor factor;
		final Unit unit;
		final FlowPropertyFactor property;

		UnitItem(
				ImpactFactor factor,
				FlowPropertyFactor property,
				Unit unit) {
			this.factor = factor;
			this.property = property;
			this.unit = unit;
		}

		@Override
		public String toString() {
			String name = unit.name;
			if (name == null)
				return "?";
			Flow f = factor.flow;
			if (f.flowPropertyFactors.size() == 1)
				return name;
			FlowProperty fp = property.flowProperty;
			return name + " - " + fp.name + "";
		}

		@Override
		public int compareTo(UnitItem other) {
			if (other == null)
				return 1;
			if (Objects.equals(this.property, other.property)
					|| this.property == null
					|| this.property.flowProperty == null
					|| other.property == null
					|| other.property.flowProperty == null)
				return Strings.compare(
						this.toString(), other.toString());
			return Strings.compare(
					this.property.flowProperty.name,
					other.property.flowProperty.name);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (this == obj)
				return true;
			if (!(obj instanceof UnitItem))
				return false;
			UnitItem other = (UnitItem) obj;
			return Objects.equals(this.unit, other.unit)
					&& Objects.equals(this.factor, other.factor)
					&& Objects.equals(this.property, other.property);
		}
	}

}
