package org.openlca.app.tools.hestia;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.MappingFileDao;
import org.openlca.core.io.maps.FlowMap;

class SettingsPanel {

	private final Button aggCheck;
	private final Spinner countSpinner;
	private final MappingCombo mappingCombo;

	SettingsPanel(Composite parent, FormToolkit tk) {
		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 8, 15, 0);

		aggCheck = UI.checkbox(comp, tk, "Search aggregated data sets");
		aggCheck.setSelection(true);

		UI.label(comp, tk, "|");

		UI.label(comp, tk, "Number of results");
		countSpinner = UI.spinner(comp, tk);
		countSpinner.setValues(25, 5, 500, 0, 5, 15);

		var db = Database.get();
		if (db == null) {
			mappingCombo = null;
		} else {
			UI.label(comp, tk, "|");
			var combo = UI.labeledCombo(comp, tk, "Mapping file");
			mappingCombo = new MappingCombo(db, combo);
		}
	}

	boolean searchAggregated() {
		return aggCheck.getSelection();
	}

	int numberOfResults() {
		return countSpinner.getSelection();
	}

	FlowMap flowMap() {
		return mappingCombo != null
				? mappingCombo.getFlowMap()
				: FlowMap.empty();
	}

	private static class MappingCombo {

		private final IDatabase db;
		private final Combo combo;
		private final List<String> mappings;

		MappingCombo(IDatabase db, Combo combo) {
			this.db = db;
			this.combo = combo;

			var items = new ArrayList<>();
			items.add(M.NoneHyphen);
			int selected = 0;

			mappings = new MappingFileDao(db).getNames()
					.stream()
					.filter(Objects::nonNull)
					.sorted()
					.toList();
			for (int i = 0; i < mappings.size(); i++) {
				var mapping = mappings.get(i);
				items.add(mapping);
				if (mapping.toLowerCase().contains("hestia")) {
					selected = i + 1;
				}
			}

			combo.setItems(items.toArray(new String[0]));
			combo.select(selected);
		}

		FlowMap getFlowMap() {
			var idx = combo.getSelectionIndex();
			if (idx == 0)
				return FlowMap.empty();
			try {
				var mapping = mappings.get(idx - 1);
				var file = new MappingFileDao(db).getForName(mapping);
				return FlowMap.of(file);
			} catch (Exception e) {
				ErrorReporter.on("failed to load flow map", e);
				return FlowMap.empty();
			}
		}
	}
}
