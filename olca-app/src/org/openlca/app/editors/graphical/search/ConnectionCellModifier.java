package org.openlca.app.editors.graphical.search;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.editors.graphical.search.ConnectionDialog.AvailableConnection;
import org.openlca.app.editors.graphical.search.ConnectionDialog.LABELS;

class ConnectionCellModifier implements ICellModifier {

	private ConnectionDialog dialog;

	ConnectionCellModifier(ConnectionDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public boolean canModify(Object element, String property) {
		if (!(element instanceof AvailableConnection))
			return false;
		AvailableConnection process = (AvailableConnection) element;
		if (property.equals(LABELS.CREATE))
			return !process.alreadyExisting && !process.connect;
		if (property.equals(LABELS.CONNECT))
			return process.connect || dialog.canBeConnected(process);
		return false;
	}

	@Override
	public Object getValue(Object element, String property) {
		if (!(element instanceof AvailableConnection))
			return null;
		AvailableConnection process = (AvailableConnection) element;
		if (property.equals(LABELS.CREATE))
			return process.create;
		if (property.equals(LABELS.CONNECT))
			return process.connect;
		return null;
	}

	@Override
	public void modify(Object element, String property, Object value) {
		if (!(element instanceof TableItem))
			return;
		TableItem item = (TableItem) element;
		if (!(item.getData() instanceof AvailableConnection))
			return;
		AvailableConnection process = (AvailableConnection) item.getData();
		if (property.equals(LABELS.CREATE))
			process.create = Boolean.parseBoolean(value.toString());
		else if (property.equals(LABELS.CONNECT)) {
			process.connect = Boolean.parseBoolean(value.toString());
			if (process.connect && !process.alreadyExisting)
				process.create = true;
		}
		dialog.viewer.refresh();
	}

}