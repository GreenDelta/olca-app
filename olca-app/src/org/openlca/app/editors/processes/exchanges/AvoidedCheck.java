package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;

class AvoidedCheck extends CheckBoxCellModifier<Exchange> {

	private final ProcessEditor editor;

	AvoidedCheck(ProcessEditor editor) {
		this.editor = editor;
	}

	@Override
	public boolean canModify(Exchange e) {
		if (e == null)
			return false;
		Process p = editor.getModel();
		if (Objects.equals(p.quantitativeReference, e))
			return false;
		if (e.flow == null)
			return false;
		FlowType type = e.flow.flowType;
		if (type == null)
			return false;
		switch (type) {
		case ELEMENTARY_FLOW:
			return false;
		case PRODUCT_FLOW:
			return (e.isAvoided && e.isInput) || (!e.isAvoided && !e.isInput);
		case WASTE_FLOW:
			return (e.isAvoided && !e.isInput) || (!e.isAvoided && e.isInput);
		default:
			return false;
		}
	}

	@Override
	protected boolean isChecked(Exchange e) {
		return e.isAvoided;
	}

	@Override
	protected void setChecked(Exchange e, boolean value) {
		if (e.isAvoided == value || !canModify(e))
			return;
		e.isAvoided = value;
		if (!value)
			e.defaultProviderId = 0;
		FlowType type = e.flow.flowType;
		if (type == FlowType.PRODUCT_FLOW)
			e.isInput = value;
		if (type == FlowType.WASTE_FLOW)
			e.isInput = !value;
		editor.setDirty(true);
	}
}