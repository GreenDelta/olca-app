package org.openlca.app.tools.openepd.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.EntityCombo;
import org.openlca.app.tools.openepd.MappingTable;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactMethod;
import org.openlca.io.openepd.io.MethodMapping;

class MappingSection {

	private final ImportDialog dialog;
	private final MethodMapping mapping;
	private TableViewer table;

	private MappingSection(ImportDialog dialog, MethodMapping mapping) {
		this.dialog = dialog;
		this.mapping = mapping;
	}

	static List<MappingSection> initAllOf(ImportDialog dialog) {
		if (dialog == null)
			return Collections.emptyList();
		return dialog.mapping.mappings()
			.stream()
			.filter(mapping -> mapping.epdMethod() != null)
			.map(mapping -> new MappingSection(dialog, mapping))
			.toList();
	}

	void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk,
			"openEPD method: " + mapping.epdMethod());
		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		var top = tk.createComposite(comp);
		UI.gridData(top, true, false);
		UI.gridLayout(top, 2, 10, 0);

		// method combo
		var combo = UI.formCombo(top, tk, "Mapped openLCA method");
		var methods = dialog.db.getAll(ImpactMethod.class);
		methods.sort(Comparator.comparing(Labels::name));
		var withNull = new ArrayList<ImpactMethod>(methods.size() + 1);
		withNull.add(null);
		withNull.addAll(methods);
		EntityCombo.of(combo, withNull)
			.select(mapping.method())
			.onSelected(method -> {
				mapping.remapWith(method);
				table.setInput(mapping.entries());
			});
		table = MappingTable.forImport(mapping).create(comp);
	}
}
