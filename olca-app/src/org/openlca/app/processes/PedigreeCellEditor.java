package org.openlca.app.processes;

import java.util.Objects;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.core.model.Exchange;

class PedigreeCellEditor extends DialogCellEditor {

	private ProcessEditor editor;
	private TableViewer viewer;
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
		} else {
			exchange = null;
			oldEntryVal = null;
			oldBaseVal = null;
		}
	}

	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		PedigreeShell shell = new PedigreeShell(cellEditorWindow.getShell(),
				exchange);
		shell.open();
		if (valuesChanged()) {
			editor.setDirty(true);
			viewer.refresh(true);
			return exchange;
		}
		return null;
	}

	private boolean valuesChanged() {
		return !Objects.equals(oldEntryVal, exchange.getPedigreeUncertainty())
				|| !Objects.equals(oldBaseVal, exchange.getBaseUncertainty());
	}
}
