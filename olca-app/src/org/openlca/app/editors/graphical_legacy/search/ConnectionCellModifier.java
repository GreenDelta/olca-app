package org.openlca.app.editors.graphical_legacy.search;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.editors.graphical_legacy.search.ConnectionDialog.LABELS;

class ConnectionCellModifier implements ICellModifier {

	private ConnectionDialog dialog;

	ConnectionCellModifier(ConnectionDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public boolean canModify(Object obj, String prop) {
		if (!(obj instanceof Candidate))
			return false;
		Candidate process = (Candidate) obj;
		if (prop.equals(LABELS.CREATE))
			return !process.processExists && !process.doConnect;
		if (prop.equals(LABELS.CONNECT))
			return process.doConnect || dialog.canBeConnected(process);
		return false;
	}

	@Override
	public Object getValue(Object obj, String prop) {
		if (!(obj instanceof Candidate))
			return null;
		Candidate c = (Candidate) obj;
		if (prop.equals(LABELS.CREATE))
			return c.doCreate;
		if (prop.equals(LABELS.CONNECT))
			return c.doConnect;
		return null;
	}

	@Override
	public void modify(Object obj, String prop, Object value) {
		if (!(obj instanceof TableItem))
			return;
		TableItem item = (TableItem) obj;
		if (!(item.getData() instanceof Candidate))
			return;
		Candidate c = (Candidate) item.getData();
		if (prop.equals(LABELS.CREATE))
			c.doCreate = Boolean.parseBoolean(value.toString());
		else if (prop.equals(LABELS.CONNECT)) {
			c.doConnect = Boolean.parseBoolean(value.toString());
			if (c.doConnect && !c.processExists) {
				c.doCreate = true;
			}
		}
		dialog.viewer.refresh();
	}

}
