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
		if (exchange == null || exchange.getFlow() == null)
			return new UnitItem[0];
		Flow flow = exchange.getFlow();
		List<UnitItem> items = new ArrayList<>();
		for (FlowPropertyFactor factor : flow.getFlowPropertyFactors()) {
			FlowProperty prop = factor.getFlowProperty();
			if (prop == null || prop.getUnitGroup() == null)
				continue;
			for (Unit unit : prop.getUnitGroup().getUnits()) {
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
		return new UnitItem(e.getUnit(), e.getFlowPropertyFactor(), e);
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
		if (Objects.equals(e.getUnit(), i.unit)
				&& Objects.equals(e.getFlowPropertyFactor(), i.factor))
			return;
		e.setUnit(i.unit);
		e.setFlowPropertyFactor(i.factor);
		editor.setDirty(true);
	}

}
