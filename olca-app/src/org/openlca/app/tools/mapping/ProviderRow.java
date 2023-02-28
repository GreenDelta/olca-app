package org.openlca.app.tools.mapping;

import java.io.File;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.model.ES1Provider;
import org.openlca.app.tools.mapping.model.ILCDProvider;
import org.openlca.app.tools.mapping.model.FlowProvider;
import org.openlca.app.tools.mapping.model.JsonProvider;
import org.openlca.app.tools.mapping.model.SimaProCsvProvider;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.io.Format;

/**
 * A provider row contains the label and actions for a connected provider of
 * source or target flows in the user interface.
 */
class ProviderRow {

	Consumer<FlowProvider> onSelect;

	ProviderRow(Composite parent, FormToolkit tk) {

		var inner = UI.formComposite(parent, tk);
		UI.gridLayout(inner, 3, 5, 0);
		var dbLink = UI.formImageHyperlink(inner, tk);
		dbLink.setImage(Icon.DATABASE.get());
		dbLink.setToolTipText("Select database");
		var fileLink = UI.formImageHyperlink(inner, tk);
		fileLink.setImage(Icon.FILE.get());
		fileLink.setToolTipText("Select file");
		var label = UI.formLabel(inner, "- none -");

		// select database as provider
		Controls.onClick(dbLink, e -> {
			var db = Database.get();
			if (db == null) {
				MsgBox.error(M.NoDatabaseOpened);
				return;
			}
			var provider = new DBProvider(db);
			fireSelect(label, provider);
		});

		// select a file as provider
		Controls.onClick(fileLink, e -> {
			var file = FileChooser.openFile()
				.withExtensions("*.zip;*.csv;*.CSV;*.xml;*.XML")
				.withTitle("Open a flow source")
				.select()
				.orElse(null);
			if (file == null)
				return;

			var provider = providerOf(file);
			if (provider == null) {
				MsgBox.error(
						"Unknown supported flow source (supported are ILCD," +
								" JSON-LD, SimaPro CSV, or EcoSpold 1 files.");
				return;
			}
			fireSelect(label, provider);
		});

	}

	private FlowProvider providerOf(File file) {
		var format = Format.detect(file).orElse(null);
		if (format == null)
			return null;
		return switch (format) {
			case ILCD_ZIP -> ILCDProvider.of(file);
			case JSON_LD_ZIP -> JsonProvider.of(file);
			case SIMAPRO_CSV -> SimaProCsvProvider.of(file);
			case ES1_XML, ES1_ZIP -> ES1Provider.of(file);
			default -> null;
		};
	}

	private void fireSelect(Label label, FlowProvider provider) {
		label.setText(label(provider));
		label.getParent().pack();
		if (onSelect != null) {
			onSelect.accept(provider);
		}
	}

	private String label(FlowProvider provider) {
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
		if (provider instanceof ES1Provider p)
			return "ecoSpold://" + p.file().getName();
		return "?";
	}
}
