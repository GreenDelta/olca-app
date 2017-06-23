package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Exchange;

class CostCellEditor extends DialogCellEditor {

	private ProcessEditor editor;
	private Exchange exchange;
	private Double oldValue;
	private String oldFormula;
	private Currency oldCurrency;

	CostCellEditor(TableViewer viewer, ProcessEditor editor) {
		super(viewer.getTable());
		this.editor = editor;
	}

	@Override
	protected void doSetValue(Object obj) {
		if (obj instanceof Exchange) {
			exchange = (Exchange) obj;
			oldValue = exchange.costs;
			oldFormula = exchange.costFormula;
			oldCurrency = exchange.currency;
			super.doSetValue(oldValue);
		} else {
			exchange = null;
			oldValue = null;
		}
	}

	@Override
	protected Object openDialogBox(Control window) {
		CostDialog.open(editor.getModel(), exchange);
		if (valuesChanged()) {
			updateContents(exchange.costs);
			editor.setDirty(true);
		}
		if (exchange.costs == null && exchange.costFormula == null)
			exchange.currency = null;
		return exchange.costs;
	}

	private boolean valuesChanged() {
		return !Objects.equals(oldValue, exchange.costs)
				|| !Objects.equals(oldFormula, exchange.costFormula)
				|| !Objects.equals(oldCurrency, exchange.currency);
	}

}
