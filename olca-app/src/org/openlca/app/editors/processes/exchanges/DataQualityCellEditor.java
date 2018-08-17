package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.editors.processes.data_quality.DataQualityShell;
import org.openlca.app.util.Error;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;

class DataQualityCellEditor extends DialogCellEditor {

	private final ProcessEditor editor;
	private final TableViewer viewer;
	private Exchange exchange;
	private String oldEntryVal;
	private Double oldBaseVal;

	public DataQualityCellEditor(TableViewer viewer, ProcessEditor editor) {
		super(viewer.getTable());
		this.viewer = viewer;
		this.editor = editor;
	}

	@Override
	protected void doSetValue(Object value) {
		if (value instanceof Exchange) {
			exchange = (Exchange) value;
			oldEntryVal = exchange.dqEntry;
			oldBaseVal = exchange.baseUncertainty;
			super.doSetValue(exchange.dqEntry);
		} else {
			exchange = null;
			oldEntryVal = null;
			oldBaseVal = null;
		}
	}

	@Override
	protected Object openDialogBox(Control control) {
		if (exchange == null || editor.getModel().exchangeDqSystem == null) {
			Error.showBox("Please select a data quality system first");
			return null;
		}
		DQSystem system = editor.getModel().exchangeDqSystem;
		String dqEntry = exchange.dqEntry;
		Double uncertainty = exchange.baseUncertainty;
		DataQualityShell shell = DataQualityShell.withUncertainty(
				control.getShell(), system, dqEntry, uncertainty);
		shell.onOk = this::onOk;
		shell.onDelete = this::onDelete;
		shell.onUseUncertainties = this::onUseUncertainties;
		shell.addDisposeListener(e -> {
			if (valuesChanged()) {
				updateContents(exchange.dqEntry);
				viewer.refresh();
				editor.setDirty(true);
			}
		});
		shell.open();
		return null;
	}

	private boolean valuesChanged() {
		return !Objects.equals(oldEntryVal, exchange.dqEntry)
				|| !Objects.equals(oldBaseVal, exchange.baseUncertainty);
	}

	private void onDelete(DataQualityShell shell) {
		exchange.dqEntry = null;
	}

	private void onOk(DataQualityShell shell) {
		exchange.dqEntry = shell.getSelection();
		exchange.baseUncertainty = shell.getBaseValue();
	}

	private void onUseUncertainties(DataQualityShell shell) {
		Uncertainty u = new Uncertainty();
		u.distributionType = UncertaintyType.LOG_NORMAL;
		u.parameter1 = exchange.amount;
		u.parameter2 = shell.updateSigmaG();
		exchange.uncertainty = u;
		viewer.refresh();
	}

}
