package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.viewers.tables.modify.CheckBoxCellModifier;
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
		var type = e.flow.flowType;
		if (type == null)
			return false;
		return switch (type) {
			case ELEMENTARY_FLOW -> false;
			case PRODUCT_FLOW ->
					(e.isAvoided && e.isInput) || (!e.isAvoided && !e.isInput);
			case WASTE_FLOW ->
					(e.isAvoided && !e.isInput) || (!e.isAvoided && e.isInput);
		};
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
		if (!value) {
			// clear the default provider for normal
			// (non-avoided) product outputs and waste
			// inputs because they are then provided
			// by the owning process
			e.defaultProviderId = 0;
		}
		// swap the exchange direction: avoided products
		// are stored as inputs; avoided wastes as outputs,
		// but they are displayed on the other sides. This
		// is because the process linking works like this
		// for these flow types, and we want to link avoided
		// flows to supply chains but with opposite signs.
		var type = e.flow.flowType;
		if (type == FlowType.PRODUCT_FLOW) {
			e.isInput = value;
		} else if (type == FlowType.WASTE_FLOW) {
			e.isInput = !value;
		}
		editor.setDirty(true);
	}
}
