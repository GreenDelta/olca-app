package org.openlca.app.editors.graphical_legacy.search;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;

class ConnectionLabelProvider extends BaseLabelProvider implements ITableLabelProvider {

	private ConnectionDialog dialog;

	ConnectionLabelProvider(ConnectionDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof Candidate))
			return null;
		Candidate con = (Candidate) obj;
		switch (col) {
		case 0:
			return Images.get(con.process);
		case 1:
			if (con.doCreate)
				return Icon.CHECK_TRUE.get();
			if (!con.processExists)
				return Icon.CHECK_FALSE.get();
			return null;
		case 2:
			if (con.doConnect)
				return Icon.CHECK_TRUE.get();
			if (dialog.canBeConnected(con))
				return Icon.CHECK_FALSE.get();
			return null;
		case 3:
			if (con.processExists)
				return Icon.ACCEPT.get();
			return null; // just show a - (getColumnText)
		case 4:
			if (con.isConnected)
				return Icon.ACCEPT.get();
			return null; // just show a - (getColumnText)
		case 5:
			if (con.isDefaultProvider)
				return Icon.ACCEPT.get();
			return null; // just show a - (getColumnText)
		default:
			return null;
		}
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof Candidate))
			return null;
		Candidate con = (Candidate) obj;
		switch (col) {
		case 0:
			return Labels.name(con.process);
		case 3:
			if (!con.processExists)
				return "-";
			return null; // show checkmark icon (getColumnImage)
		case 4:
			if (!con.isConnected)
				return "-";
			return null; // show checkmark icon (getColumnImage)
		case 5:
			if (!con.isDefaultProvider)
				return "-";
			return null; // show checkmark icon (getColumnImage)
		}
		return null;
	}

}
