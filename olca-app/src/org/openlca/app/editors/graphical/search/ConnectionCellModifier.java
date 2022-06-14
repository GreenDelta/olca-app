package org.openlca.app.editors.graphical.search;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.TableItem;

import static org.openlca.app.editors.graphical.search.ConnectionDialog.LABELS.CONNECT;
import static org.openlca.app.editors.graphical.search.ConnectionDialog.LABELS.CREATE;

class ConnectionCellModifier implements ICellModifier {

	private final ConnectionDialog dialog;

	ConnectionCellModifier(ConnectionDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public boolean canModify(Object obj, String prop) {
		if (!(obj instanceof Candidate process))
			return false;
		if (prop.equals(CREATE))
			return !process.processExists && !process.doConnect;
		if (prop.equals(CONNECT))
			return process.doConnect || dialog.canBeConnected(process);
		return false;
	}

	@Override
	public Object getValue(Object obj, String prop) {
		if (!(obj instanceof Candidate c))
			return null;
		if (prop.equals(CREATE))
			return c.doCreate;
		if (prop.equals(CONNECT))
			return c.doConnect;
		return null;
	}

	@Override
	public void modify(Object obj, String prop, Object value) {
		if (!(obj instanceof TableItem item))
			return;
		if (!(item.getData() instanceof Candidate c))
			return;
		if (prop.equals(CREATE))
			c.doCreate = Boolean.parseBoolean(value.toString());
		else if (prop.equals(CONNECT)) {
			c.doConnect = Boolean.parseBoolean(value.toString());
			if (c.doConnect && !c.processExists) {
				c.doCreate = true;
			}
		}
		dialog.viewer.refresh();
	}

}
