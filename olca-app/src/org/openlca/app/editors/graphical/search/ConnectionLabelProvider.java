package org.openlca.app.editors.graphical.search;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;

class ConnectionLabelProvider extends BaseLabelProvider implements ITableLabelProvider {

	private final ConnectionDialog dialog;

	ConnectionLabelProvider(ConnectionDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof LinkCandidate c))
			return null;
		switch (col) {
		case 0:
			return Images.get(c.process);
		case 1:
			if (c.doCreate)
				return Icon.CHECK_TRUE.get();
			if (!c.isInSystem && dialog.canBeConnected(c))
				return Icon.CHECK_FALSE.get();
			return null;
		case 2:
			if (c.doConnect)
				return Icon.CHECK_TRUE.get();
			if (dialog.canBeConnected(c))
				return Icon.CHECK_FALSE.get();
			return null;
		case 3:
			return c.isInSystem ? Icon.ACCEPT.get() : null;
		case 4:
			return c.isConnected ? Icon.ACCEPT.get() : null;
		case 5:
			return c.isDefaultProvider ? Icon.ACCEPT.get() : null;
		default:
			return null;
		}
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof LinkCandidate c))
			return null;
		return switch (col) {
			case 0 -> Labels.name(c.process);
			case 3 -> !c.isInSystem ? "-" : null;
			case 4 -> !c.isConnected ? "-" : null;
			case 5 -> !c.isDefaultProvider ? "-" : null;
			default -> null;
		};
	}

}
