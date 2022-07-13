package org.openlca.app.tools.mapping;

import java.io.File;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.model.ILCDProvider;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.tools.mapping.model.JsonProvider;
import org.openlca.app.tools.mapping.model.ProviderType;
import org.openlca.app.tools.mapping.model.SimaProCsvProvider;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;

/**
 * A provider row contains the label and actions for a connected provider of
 * source or target flows in the user interface.
 */
class ProviderRow {

	Consumer<IProvider> onSelect;

	ProviderRow(Composite parent, FormToolkit tk) {

		Composite inner = tk.createComposite(parent);
		UI.gridLayout(inner, 3, 5, 0);
		ImageHyperlink dbLink = tk.createImageHyperlink(inner, SWT.NONE);
		dbLink.setImage(Icon.DATABASE.get());
		dbLink.setToolTipText("Select database");
		ImageHyperlink fileLink = tk.createImageHyperlink(inner, SWT.NONE);
		fileLink.setImage(Icon.FILE.get());
		fileLink.setToolTipText("Select file");
		Label label = UI.formLabel(inner, "- none -");

		// select database as provider
		Controls.onClick(dbLink, e -> {
			IDatabase db = Database.get();
			if (db == null) {
				MsgBox.error(M.NoDatabaseOpened);
				return;
			}
			DBProvider provider = new DBProvider(db);
			fireSelect(label, provider);
		});

		// select a file as provider
		Controls.onClick(fileLink, e -> {
			File file = FileChooser.openFile()
				.withExtensions("*.zip;*.csv;*.CSV")
				.withTitle("Open a flow source")
				.select()
				.orElse(null);
			if (file == null)
				return;
			var type = ProviderType.of(file);
			IProvider provider = switch (type) {
				case ILCD_ZIP -> ILCDProvider.of(file);
				case JSON_ZIP -> JsonProvider.of(file);
				case SIMAPRO_CSV -> SimaProCsvProvider.of(file);
				default -> null;
			};
			if (provider == null) {
				MsgBox.error("Unknown flow source (ILCD "
						+ "or JSON-LD packages are supported).");
				return;
			}
			fireSelect(label, provider);
		});

	}

	private void fireSelect(Label label, IProvider provider) {
		label.setText(label(provider));
		label.getParent().pack();
		if (onSelect != null) {
			onSelect.accept(provider);
		}
	}

	private String label(IProvider provider) {
		if (provider == null)
			return "- none -";
		if (provider instanceof DBProvider p)
			return "db://" + p.db().getName();
		if (provider instanceof JsonProvider p )
			return "jsonld://" + p.file().getName();
		if (provider instanceof ILCDProvider p)
			return "ilcd://" + p.file().getName();
		if (provider instanceof SimaProCsvProvider p)
			return "simapro://" + p.file().getName();
		return "?";
	}
}
