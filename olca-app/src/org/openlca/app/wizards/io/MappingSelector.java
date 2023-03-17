package org.openlca.app.wizards.io;


import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.MappingFileDao;
import org.openlca.core.io.maps.FlowMap;

class MappingSelector {

	private final IDatabase db;
	private final Consumer<FlowMap> handler;

	private MappingSelector(IDatabase db, Consumer<FlowMap> handler) {
		this.db	 = db;
		this.handler = handler;
	}

	static MappingSelector on(Consumer<FlowMap> handler) {
		return new MappingSelector(Database.get(), handler);
	}

	void render(Composite comp) {
		UI.label(comp, "Flow mapping");

		// initialize the combo box
		var combo = new Combo(comp, SWT.READ_ONLY);
		UI.fillHorizontal(combo);
		UI.fillHorizontal(comp);
		var dbFiles = new MappingFileDao(db)
				.getNames()
				.stream()
				.sorted()
				.toList();
		var items = new String[dbFiles.size() + 1];
		items[0] = "";
		for (int i = 0; i < dbFiles.size(); i++) {
			items[i + 1] = dbFiles.get(i);
		}
		combo.setItems(items);
		combo.select(0);
		Controls.onSelect(combo, $ -> onItemSelected(combo));

		// add the file button
		var fileBtn = new Button(comp, SWT.NONE);
		fileBtn.setText("From file");
		fileBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
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
			onItemSelected(combo);
		});

	}

	private void onItemSelected(Combo combo) {
		if (handler == null)
			return;
		int idx = combo.getSelectionIndex();

		// none
		if (idx == 0) {
			handler.accept(null);
			return;
		}

		var mapping = combo.getItem(idx);
		try {

			// db mapping
			var dbMap = new MappingFileDao(db).getForName(mapping);
			if (dbMap != null) {
				handler.accept(FlowMap.of(dbMap));
				return;
			}

			// file mapping
			var file = new File(mapping);
			if (file.exists()) {
				handler.accept(FlowMap.fromCsv(file));
			}

		} catch (Exception e) {
			ErrorReporter.on("Failed to open mapping: " + mapping, e);
		}
	}
}
