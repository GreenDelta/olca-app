package org.openlca.app.editors.processes.exchanges;

import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.core.model.Exchange;

class Clipboard {

	static String converter(TableItem item, int col) {
		if (item == null)
			return "";
		if (col != 2)
			return TableClipboard.text(item, col);
		Object data = item.getData();
		if (!(data instanceof Exchange))
			return TableClipboard.text(item, col);
		Exchange e = (Exchange) data;
		return Double.toString(e.amount);
	}

}
