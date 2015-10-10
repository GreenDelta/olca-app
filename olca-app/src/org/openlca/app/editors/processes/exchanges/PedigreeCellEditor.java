package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.core.model.Exchange;

class PedigreeCellEditor extends DialogCellEditor {

	private ProcessEditor editor;
	private Exchange exchange;
	private String oldEntryVal;
	private Double oldBaseVal;

	public PedigreeCellEditor(TableViewer viewer, ProcessEditor editor) {
		super(viewer.getTable());
		this.editor = editor;
	}

	@Override
	protected void doSetValue(Object value) {
		if (value instanceof Exchange) {
			exchange = (Exchange) value;
			oldEntryVal = exchange.getPedigreeUncertainty();
			oldBaseVal = exchange.getBaseUncertainty();
			super.doSetValue(exchange.getPedigreeUncertainty());
		} else {
			exchange = null;
			oldEntryVal = null;
			oldBaseVal = null;
		}
	}

	@Override
	protected Object openDialogBox(Control control) {
		PedigreeShell shell = new PedigreeShell(control.getShell(), exchange);
		shell.addDisposeListener(e -> {
			if (valuesChanged()) {
				updateContents(exchange.getPedigreeUncertainty());
				editor.setDirty(true);
			}
		});
		shell.open();
		return null;
	}

	private boolean valuesChanged() {
		return !Objects.equals(oldEntryVal, exchange.getPedigreeUncertainty())
				|| !Objects.equals(oldBaseVal, exchange.getBaseUncertainty());
	}
}
