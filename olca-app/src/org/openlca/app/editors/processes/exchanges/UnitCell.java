package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.editors.processes.exchanges.UnitCell.UnitItem;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Unit;
import org.openlca.util.Strings;

class UnitCell extends ComboBoxCellModifier<Exchange, UnitItem> {

	private final ProcessEditor editor;

	UnitCell(ProcessEditor editor) {
		this.editor = editor;
	}

	@Override
	protected UnitItem[] getItems(Exchange exchange) {
		if (exchange == null || exchange.flow == null)
			return new UnitItem[0];
		Flow flow = exchange.flow;
		List<UnitItem> items = new ArrayList<>();
		for (FlowPropertyFactor factor : flow.flowPropertyFactors) {
			FlowProperty prop = factor.flowProperty;
			if (prop == null || prop.unitGroup == null)
				continue;
			for (Unit unit : prop.unitGroup.units) {
				UnitItem i = new UnitItem(unit, factor, exchange);
				items.add(i);
			}
		}
		Collections.sort(items);
		return items.toArray(new UnitItem[0]);
	}

	@Override
	protected UnitItem getItem(Exchange e) {
		if (e == null)
			return null;
		return new UnitItem(e.unit, e.flowPropertyFactor, e);
	}

	@Override
	protected String getText(UnitItem val) {
		if (val == null)
			return "";
		return val.toString();
	}

	@Override
	protected void setItem(Exchange e, UnitItem i) {
		if (e == null || i == null)
			return;
		if (Objects.equals(e.unit, i.unit)
				&& Objects.equals(e.flowPropertyFactor, i.factor))
			return;
		e.unit = i.unit;
		e.flowPropertyFactor = i.factor;
		editor.setDirty(true);
	}

	record UnitItem(Unit unit, FlowPropertyFactor factor, Exchange exchange)
		implements Comparable<UnitItem> {

		@Override
		public String toString() {
			String name = unit.name;
			if (name == null)
				return "?";
			Flow f = exchange.flow;
			if (f.flowPropertyFactors.size() == 1)
				return name;
			FlowProperty fp = factor.flowProperty;
			return name + " - " + fp.name + "";
		}

		@Override
		public int compareTo(UnitItem other) {
			// do some null checks, because sometimes
			// the data may are in a corrupt / editing state
			if (other == null || other.unit == null || other.factor == null)
				return 1;
			if (unit == null || factor == null)
				return -1;
			if (exchange.flowPropertyFactor == null)
				return 0;

			FlowProperty thisFp = factor.flowProperty;
			FlowProperty otherFp = other.factor.flowProperty;
			FlowProperty exchFp = exchange.flowPropertyFactor.flowProperty;

			if (Objects.equals(thisFp, otherFp))
				return Strings.compare(this.toString(), other.toString());
			if (Objects.equals(thisFp, exchFp))
				return -1;
			if (Objects.equals(otherFp, exchFp))
				return 1;
			else
				return Strings.compare(thisFp.name, otherFp.name);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (this == obj)
				return true;
			if (!(obj instanceof UnitItem other))
				return false;
			return Objects.equals(this.unit, other.unit)
					&& Objects.equals(this.factor, other.factor)
					&& Objects.equals(this.exchange, other.exchange);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(toString());
		}
	}
}
