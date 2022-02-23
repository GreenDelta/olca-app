package org.openlca.app.editors.processes.exchanges;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.Labels;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ModelType;

class LocationCell extends DialogCellEditor {

	private final ProcessEditor editor;
	private Exchange exchange;

	LocationCell(Composite parent, ProcessEditor editor) {
		super(parent);
		this.editor = editor;
	}

	@Override
	protected void doSetValue(Object value) {
		if (!(value instanceof Exchange)) {
			super.doSetValue("");
			exchange = null;
			return;
		}
		exchange = (Exchange) value;
		String s = Labels.name(exchange.location);
		super.doSetValue(s == null ? "" : s);
	}

	@Override
	protected Object openDialogBox(Control control) {
		if (exchange == null)
			return null;
		ModelSelector dialog = new ModelSelector(
				ModelType.LOCATION);
		dialog.isEmptyOk = true;
		if (dialog.open() != Window.OK)
			return null;

		var loc = dialog.first();

		// clear the location
		if (loc == null) {
			if (exchange.location == null)
				return null;
			// delete the location
			exchange.location = null;
			editor.setDirty(true);
			return exchange;
		}

		// the same location was selected again
		if (exchange.location != null
				&& exchange.location.id == loc.id)
			return null;

		// a new location was selected
		LocationDao dao = new LocationDao(Database.get());
		exchange.location = dao.getForId(loc.id);
		editor.setDirty(true);
		return exchange;
	}

}
