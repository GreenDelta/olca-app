package org.openlca.app.editors.processes.exchanges;

import java.util.Objects;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.Labels;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

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
		String s = Labels.getDisplayName(exchange.location);
		super.doSetValue(s == null ? "" : s);
	}

	@Override
	protected Object openDialogBox(Control control) {
		Location initial = exchange == null
				? null
				: exchange.location;
		CategorizedDescriptor loc = ModelSelectionDialog.select(
				ModelType.LOCATION);
		if (loc == null)
			return null;
		LocationDao dao = new LocationDao(Database.get());
		Location location = dao.getForId(loc.id);
		if (exchange != null) {
			exchange.location = location;
		}
		if (!Objects.equals(initial, location)) {
			editor.setDirty(true);
		}
		return exchange;
	}

}
