package org.openlca.app.tools.openepd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.EntityCombo;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.io.openepd.input.ImpactMapping;
import org.openlca.io.openepd.input.IndicatorKey;

class ImpactSection {

	private final ImportDialog dialog;
	private final String methodCode;
	private final ImpactMapping mapping;
	private TableViewer table;

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
		var methods = dialog.db.getAll(ImpactMethod.class);
		methods.sort(Comparator.comparing(Labels::name));
		var withNull = new ArrayList<ImpactMethod>(methods.size() + 1);
		withNull.add(null);
		withNull.addAll(methods);
		var current = mapping.getMethodMapping(methodCode);
		EntityCombo.of(combo, withNull)
			.select(current.method())
			.onSelected(method -> {
				var next = mapping.swapMethod(methodCode, method);
				if (table != null) {
					table.setInput(next.keys());
				}
				dialog.setMappingChanged();
			});

		table = Tables.createViewer(comp,
			"openEPD Code",
			"openEPD unit",
			"openLCA indicator",
			"openLCA code",
			"openLCA unit");
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);
		table.setLabelProvider(new MappingLabel());
		new ModifySupport<IndicatorKey>(table)
			.bind("openLCA indicator", new ImpactModifier());
		table.setInput(current.keys());
	}

	private class ImpactModifier extends
		ComboBoxCellModifier<IndicatorKey, ImpactCategory> {

		@Override
		protected ImpactCategory[] getItems(IndicatorKey key) {
			var m = mapping.getMethodMapping(methodCode);
			if (m.isEmpty())
				return new ImpactCategory[0];
			var impacts = m.method().impactCategories;
			impacts.sort(Comparator.comparing(Labels::name));
			var array = new ImpactCategory[impacts.size() + 1];
			for (int i = 0; i < impacts.size(); i++) {
				array[i + 1] = impacts.get(i);
			}
			return array;
		}

		@Override
		protected ImpactCategory getItem(IndicatorKey key) {
			var m = mapping.getIndicatorMapping(methodCode, key);
			return m.indicator();
		}

		@Override
		protected String getText(ImpactCategory impact) {
			return Labels.name(impact);
		}

		@Override
		protected void setItem(IndicatorKey key, ImpactCategory impact) {
			mapping.swapIndicator(methodCode, key, impact);
			dialog.setMappingChanged();
		}
	}

	private class MappingLabel extends LabelProvider
		implements ITableLabelProvider, ITableFontProvider, ITableColorProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof IndicatorKey key))
				return null;
			var m = mapping.getIndicatorMapping(methodCode, key);
			return switch (col) {
				case 0, 1 -> Icon.BUILDING.get();
				case 2 -> Images.get(ModelType.IMPACT_CATEGORY);
				default -> m.isEmpty()
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
					: "select an indicator";
				case 3 -> !m.isEmpty()
					? m.indicator().code
					: null;
				case 4 -> !m.isEmpty()
					? m.indicator().referenceUnit
					: null;
				default -> null;
			};
		}

		@Override
		public Font getFont(Object obj, int col) {
			if (col != 2 || !(obj instanceof IndicatorKey key))
				return null;
			var m = mapping.getIndicatorMapping(methodCode, key);
			return m.isEmpty()
				? UI.italicFont()
				: null;
		}

		@Override
		public Color getBackground(Object obj, int col) {
			return null;
		}

		@Override
		public Color getForeground(Object obj, int col) {
			if (col != 2 || !(obj instanceof IndicatorKey key))
				return null;
			var m = mapping.getIndicatorMapping(methodCode, key);
			return m.isEmpty()
				? Colors.linkBlue()
				: null;
		}
	}
}
