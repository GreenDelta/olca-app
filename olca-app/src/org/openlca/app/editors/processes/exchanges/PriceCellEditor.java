package org.openlca.app.editors.processes.exchanges;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.core.model.Exchange;

class PriceCellEditor extends DialogCellEditor {

	private ProcessEditor editor;
	private Exchange exchange;
	private Double oldValue;

	PriceCellEditor(TableViewer viewer, ProcessEditor editor) {
		super(viewer.getTable());
		this.editor = editor;
	}

	@Override
	protected void doSetValue(Object obj) {
		if (obj instanceof Exchange) {
			exchange = (Exchange) obj;
			// TODO: oldValue = exchange.price;
			super.doSetValue(1.0);
		} else {
			exchange = null;
			oldValue = null;
		}
	}

	@Override
	protected Object openDialogBox(Control window) {
		PriceDialog.open(exchange);

		return null;
	}

}
