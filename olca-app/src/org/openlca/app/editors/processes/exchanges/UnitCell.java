package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Unit;

class UnitCell extends ComboBoxCellModifier<Exchange, UnitItem> {

	private ProcessEditor editor;

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
		return items.toArray(new UnitItem[items.size()]);
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

}
