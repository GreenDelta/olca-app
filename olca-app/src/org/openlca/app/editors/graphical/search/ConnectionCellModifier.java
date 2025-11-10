package org.openlca.app.editors.graphical.search;

import static org.openlca.app.editors.graphical.search.ConnectionDialog.LABELS.CONNECT;
import static org.openlca.app.editors.graphical.search.ConnectionDialog.LABELS.CREATE;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.util.ErrorReporter;

class ConnectionCellModifier implements ICellModifier {

	private final ConnectionDialog dialog;

	ConnectionCellModifier(ConnectionDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public boolean canModify(Object obj, String prop) {
		if (!(obj instanceof LinkCandidate c))
			return false;
		if (prop.equals(CREATE))
			return !c.isInSystem && (c.doCreate || dialog.canBeAdded(c));
		if (prop.equals(CONNECT))
			return c.doConnect || dialog.canBeConnected(c);
		return false;
	}

	@Override
	public Object getValue(Object obj, String prop) {
		if (!(obj instanceof LinkCandidate c))
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
		if (!(item.getData() instanceof LinkCandidate c))
			return;

		boolean b;
		try {
			b = Boolean.parseBoolean(value.toString());
		} catch (Exception e) {
			ErrorReporter.on("Failed to parse boolean: " + value, e);
			return;
		}

		if (prop.equals(CREATE)) {
			setCreate(c, b);
		} else if (prop.equals(CONNECT)) {
			setConnect(c, b);
		}
		dialog.viewer.refresh();
	}

	private void setCreate(LinkCandidate c, boolean b) {
		if (c.doCreate == b)
			return;
		c.doCreate = b;
		if (b) {
			c.doConnect = dialog.canBeConnected(c);
		} else if (c.doConnect) {
			// it can be only connected if it is in the system
			c.doConnect = c.isInSystem;
		}
	}

	private void setConnect(LinkCandidate c, boolean b) {
		if (c.doConnect == b)
			return;
		c.doConnect = b;
		if (b && !c.isInSystem) {
			// if it is connected but not in the system,
			// it must be created
			c.doCreate = true;
		}
	}

}
