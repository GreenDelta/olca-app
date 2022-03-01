package org.openlca.app.tools.openepd.input;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.EntityCombo;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;

import java.util.Collections;
import java.util.List;

class ImpactSection {

	private final ImportDialog dialog;
	private final String methodCode;
	private final ImportMapping mapping;

	private ImpactSection(ImportDialog dialog, String methodCode) {
		this.dialog = dialog;
		this.methodCode = methodCode;
		this.mapping = dialog.mapping;
	}

	static List<ImpactSection> initAllOf(ImportDialog dialog) {
		if (dialog == null)
			return Collections.emptyList();
		return dialog.mapping.methodCodes()
			.stream()
			.map(code -> new ImpactSection(dialog, code))
			.toList();
	}

	void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "openEPD method: " + methodCode);
		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		var top = tk.createComposite(comp);
		UI.gridData(top, true, false);
		UI.gridLayout(top, 2, 10, 0);

		// method combo
		var combo = UI.formCombo(top, tk, "Mapped openLCA method");
		var methods = dialog.db.allOf(ImpactMethod.class);
		var current = mapping.getMethodMapping(methodCode);
		EntityCombo.of(combo, methods)
			.select(current.method())
			.onSelected(method -> {
				var next = mapping.swapMethod(methodCode, method);
				// TODO update table
			});

		var table = Tables.createViewer(combo,
				"openEPD Code",
				"openEPD unit",
				"openLCA indicator",
				"openLCA code",
				"openLCA unit");
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);
		table.setLabelProvider(new MappingLabel());
		table.setInput(current.keys());
	}

	private class MappingLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof IndicatorKey key))
				return null;
			var m = mapping.getIndicatorMapping(methodCode, key);
			return switch (col) {
				case 0, 1 -> Icon.BUILDING.get();
				default ->  m.isEmpty()
					? null
					: Images.get(ModelType.IMPACT_CATEGORY);
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof IndicatorKey key))
				return null;
			var m = mapping.getIndicatorMapping(methodCode, key);
			return switch (col) {
				case 0 -> key.code();
				case 1 -> key.unit();
				case 2 -> !m.isEmpty()
					? Labels.name(m.indicator())
					: null;
				case 3 -> !m.isEmpty()
					? m.indicator().code
					: null;
				case 4 -> !m.isEmpty()
					? m.indicator().referenceUnit
					: null;
				default -> null;
			};
		}
	}


}
