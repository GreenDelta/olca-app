package org.openlca.app.wizards.io;


import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.MappingFileDao;
import org.openlca.io.maps.FlowMap;

class MappingFileCombo {

	private final IDatabase db;
	private final Combo combo;
	private Consumer<FlowMap> listener;

	private MappingFileCombo(Composite parent, IDatabase db) {
		this.db = db;
		var comp = new Composite(parent, SWT.NONE);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 2, 5, 0);

		// initialize the combo box
		combo = new Combo(comp, SWT.READ_ONLY);
		UI.gridData(combo, true, false);
		var dbFiles = new MappingFileDao(db)
			.getNames()
			.stream()
			.sorted()
			.collect(Collectors.toList());
		var items = new String[dbFiles.size() + 1];
		items[0] = "";
		for (int i = 0; i < dbFiles.size(); i++) {
			items[i + 1] = dbFiles.get(i);
		}
		combo.setItems(items);
		combo.select(0);
		Controls.onSelect(combo, $ -> onItemSelected());

		// add the file button
		var fileBtn = new Button(comp, SWT.NONE);
		fileBtn.setText("From file");
		Controls.onSelect(fileBtn, e -> {
			var file = FileChooser.open("*.csv");
			if (file == null)
				return;
			var oldItems = combo.getItems();
			var nextItems = Arrays.copyOf(oldItems, oldItems.length + 1);
			var idx = nextItems.length - 1;
			nextItems[idx] = file.getAbsolutePath();
			combo.setItems(nextItems);
			combo.select(idx); // does not fire an event
			onItemSelected();
		});

	}

	static MappingFileCombo create(Composite parent, IDatabase db) {
		return new MappingFileCombo(parent, db);
	}

	void onSelected(Consumer<FlowMap> listener) {
		this.listener = listener;
	}

	private void onItemSelected() {
		if (listener == null)
			return;
		int idx = combo.getSelectionIndex();

		// none
		if (idx == 0) {
			listener.accept(null);
			return;
		}

		var mapping = combo.getItem(idx);
		try {

			// db mapping
			var dbMap = new MappingFileDao(db).getForName(mapping);
			if (dbMap != null) {
				listener.accept(FlowMap.of(dbMap));
				return;
			}

			// file mapping
			var file = new File(mapping);
			if (file.exists()) {
				listener.accept(FlowMap.fromCsv(file));
			}

		}catch (Exception e) {
			ErrorReporter.on("Failed to open mapping: " + mapping, e);
		}
	}
}
