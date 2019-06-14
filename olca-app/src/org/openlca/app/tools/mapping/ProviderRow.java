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
import org.openlca.app.util.Controls;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;

/**
 * A provider row contains the label and actions for a connected provider of
 * source or target flows in the user interface.
 */
class ProviderRow {

	Consumer<IProvider> onSelect;
	Runnable onSync;

	ProviderRow(Composite parent, FormToolkit tk) {

		Composite inner = tk.createComposite(parent);
		UI.gridLayout(inner, 4, 5, 0);
		ImageHyperlink dbLink = tk.createImageHyperlink(inner, SWT.NONE);
		dbLink.setImage(Icon.DATABASE.get());
		dbLink.setToolTipText("Select database");
		ImageHyperlink fileLink = tk.createImageHyperlink(inner, SWT.NONE);
		fileLink.setImage(Icon.FILE.get());
		fileLink.setToolTipText("Select file");
		Label label = UI.formLabel(inner, "- none -");
		ImageHyperlink syncLink = tk.createImageHyperlink(inner, SWT.NONE);
		syncLink.setImage(Icon.REFRESH.get());
		syncLink.setToolTipText("Synchronize flows ...");
		syncLink.setVisible(false);
		Controls.onClick(syncLink, e -> {
			if (onSync != null)
				onSync.run();
		});

		// select database as provider
		Controls.onClick(dbLink, e -> {
			IDatabase db = Database.get();
			if (db == null) {
				Error.showBox(M.NoDatabaseOpened);
				return;
			}
			DBProvider provider = new DBProvider(db);
			fireSelect(label, provider);
			syncLink.setVisible(true);
		});

		// select a file as provider
		Controls.onClick(fileLink, e -> {
			File file = FileChooser.forImport("*.zip");
			if (file == null)
				return;
			ProviderType type = ProviderType.of(file);
			IProvider provider = null;
			switch (type) {
			case ILCD_PACKAGE:
				provider = ILCDProvider.of(file);
				break;
			case JSON_LD_PACKAGE:
				provider = JsonProvider.of(file);
				break;
			default:
				break;
			}
			if (provider == null) {
				Error.showBox("Unknown flow source (ILCD "
						+ "or JSON-LD packages are supported).");
				return;
			}
			fireSelect(label, provider);
			syncLink.setVisible(true);
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
		if (provider instanceof DBProvider)
			return "db://" + ((DBProvider) provider).db.getName();
		if (provider instanceof JsonProvider)
			return "jsonld://" + ((JsonProvider) provider).file.getName();
		if (provider instanceof ILCDProvider)
			return "ilcd://" + ((ILCDProvider) provider).file.getName();
		return "?";
	}
}