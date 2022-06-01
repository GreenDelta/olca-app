package org.openlca.app.tools.openepd.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
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
import org.openlca.io.openepd.io.IndicatorMapping;
import org.openlca.io.openepd.io.MethodMapping;
import org.openlca.util.Strings;

class ImpactSection {

	private final ImportDialog dialog;
	private final MethodMapping mapping;
	private TableViewer table;

	private ImpactSection(ImportDialog dialog, MethodMapping mapping) {
		this.dialog = dialog;
		this.mapping = mapping;
	}

	static List<ImpactSection> initAllOf(ImportDialog dialog) {
		if (dialog == null)
			return Collections.emptyList();
		return dialog.mapping.mappings()
			.stream()
			.filter(mapping -> mapping.epdMethod() != null)
			.map(mapping -> new ImpactSection(dialog, mapping))
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

		createTable(comp);
	}

	private void createTable(Composite comp) {
		var columns = new String[5 + mapping.scopes().size()];
		columns[0] = "openEPD Indicator";
		columns[1] = "openEPD Unit";
		columns[2] = "Indicator";
		columns[3] = "Unit";
		columns[4] = "Factor";
		for (int i = 0; i < mapping.scopes().size(); i++) {
			columns[i + 5] = mapping.scopes().get(i);
		}
		table = Tables.createViewer(comp, columns);
		table.setLabelProvider(new MappingLabel());
		var widths = new double[columns.length];
		widths[0] = 0.1;
		widths[1] = 0.1;
		widths[2] = 0.2;
		widths[3] = 0.1;
		widths[4] = 0.1;
		Arrays.fill(widths, 5, columns.length,
			.4 / (mapping.scopes().size()));
		Tables.bindColumnWidths(table,widths);
		for (int i = 4; i < columns.length; i++) {
			table.getTable()
				.getColumn(i)
				.setAlignment(SWT.CENTER);
		}
		table.setInput(mapping.entries());

		new ModifySupport<IndicatorMapping>(table)
			.bind("openLCA indicator", new ImpactModifier());
		table.setInput(mapping.entries());
	}

	private class ImpactModifier extends
		ComboBoxCellModifier<IndicatorMapping, ImpactCategory> {

		@Override
		protected ImpactCategory[] getItems(IndicatorMapping row) {
			var method = mapping.method();
			if (method == null)
				return new ImpactCategory[0];
			var impacts = method.impactCategories;
			impacts.sort(Comparator.comparing(Labels::name));
			var array = new ImpactCategory[impacts.size() + 1];
			for (int i = 0; i < impacts.size(); i++) {
				array[i + 1] = impacts.get(i);
			}
			return array;
		}

		@Override
		protected ImpactCategory getItem(IndicatorMapping row) {
			return row.indicator();
		}

		@Override
		protected String getText(ImpactCategory impact) {
			return Labels.name(impact);
		}

		@Override
		protected void setItem(IndicatorMapping row, ImpactCategory impact) {
			if (Objects.equals(impact, row.indicator()))
				return;
			var epdInd = row.epdIndicator();
			if (epdInd == null)
				return;

			if (impact == null) {
				row.indicator(null)
					.unit(null)
					.factor(1);
				return;
			}

			var unit = epdInd.unitMatchOf(impact.referenceUnit).orElse(null);
			row.indicator(impact)
				.unit(unit)
				.factor(unit != null
					? 1 / unit.factor()
					: 1);
		}
	}

	private class MappingLabel extends LabelProvider
		implements ITableLabelProvider, ITableColorProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof IndicatorMapping m))
				return null;
			return switch (col) {
				case 0, 1 -> Icon.BUILDING.get();
				case 2 -> Images.get(ModelType.IMPACT_CATEGORY);
				default -> m.indicator() != null
					? null
					: Images.get(ModelType.IMPACT_CATEGORY);
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof IndicatorMapping m))
				return null;
			var epdInd = m.epdIndicator();
			return switch (col) {
				case 0 -> epdInd != null
					? epdInd.code() + " - " + epdInd.name()
					: null;
				case 1 -> epdInd != null
					? epdInd.unit()
					: null;
				case 2 -> m.indicator() != null
					? Labels.name(m.indicator())
					: " - ";
				case 3 -> !m.isEmpty()
					? m.indicator().code
					: null;
				case 4 -> m.indicator() != null
					? m.indicator().referenceUnit
					: null;
				default -> null;
			};
		}

		@Override
		public Color getBackground(Object obj, int col) {
			return null;
		}

		@Override
		public Color getForeground(Object obj, int col) {
			if (!(obj instanceof IndicatorKey key))
				return null;
			var m = mapping.getIndicatorMapping(methodCode, key);
			if (col == 2 && m.isEmpty()) {
				return Colors.linkBlue();
			}
			if (col == 3 && !m.isEmpty() &&
				!Strings.nullOrEqual(m.indicator().code, key.code())) {
				return Colors.systemColor(SWT.COLOR_RED);
			}
			if (col == 4 && !m.isEmpty() &&
				!Strings.nullOrEqual(m.indicator().referenceUnit, key.unit())) {
				return Colors.systemColor(SWT.COLOR_RED);
			}
			return null;
		}
	}
}
