package org.openlca.app.tools.openepd.output;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.tools.openepd.MappingTable;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.io.openepd.io.MethodMapping;
import org.openlca.io.openepd.io.Vocab.Method;

record MappingSection(MethodMapping mapping) {

	void render(Composite body, FormToolkit tk) {
		var title = mapping.method() != null
			? "Results: " + Labels.name(mapping.method())
			: "Results";
		var section = UI.section(body, tk, title);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		createCombo(comp, tk);
		MappingTable.forExport(mapping).create(comp);
	}

	private void createCombo(Composite parent, FormToolkit tk) {
		var comp = UI.formComposite(parent, tk);
		var combo = UI.formCombo(comp, tk, "openEPD LCIA Method");
		var methods = Method.values();
		var items = new String[methods.length];
		int selectionIdx = -1;
		Method selected = null;
		double score = 0;
		for (int i = 0; i < methods.length; i++) {
			var method = methods[i];
			items[i] = method.code();
			if (method == Method.UNKNOWN_LCIA
				&& selected == null) {
				selectionIdx = i;
				selected = method;
				continue;
			}
			if (mapping.method() != null) {
				var s = method.matchScoreOf(mapping.method().name);
				if (s > score) {
					selectionIdx = i;
					selected = method;
					score = s;
				}
			}
		}

		mapping.epdMethod(selected);
		combo.setItems(items);
		combo.select(selectionIdx);
		Controls.onSelect(combo, $ -> {
			var idx = combo.getSelectionIndex();
			mapping.epdMethod(methods[idx]);
		});
	}
}
