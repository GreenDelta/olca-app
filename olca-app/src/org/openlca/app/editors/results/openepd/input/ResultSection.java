package org.openlca.app.editors.results.openepd.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.EntityCombo;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.CalculationType;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ResultModel;
import org.openlca.util.Strings;

class ResultSection {

	private final ImportDialog dialog;
	private final String epdMethod;
	private final String epdScope;
	private final ResultModel result;
	private final List<MappedValue> mappedValues;

	private ResultSection(
		ImportDialog dialog, String epdMethod, String epdScope) {
		this.dialog = dialog;
		this.epdMethod = epdMethod;
		this.epdScope = epdScope;
		this.result = new ResultModel();
		this.result.name = dialog.epd.name + " - " + epdScope + " - " + epdMethod;
		this.result.setup = new CalculationSetup()
			.withType(CalculationType.SIMPLE_CALCULATION);
		mappedValues = initMappings();
	}

	/**
	 * Initializes a result section for each method and scope pair that can
	 * be found in the EPD of the given import dialog.
	 */
	static List<ResultSection> initAllOf(ImportDialog dialog) {
		if (dialog == null)
			return Collections.emptyList();

		var sections = new ArrayList<ResultSection>();
		var epd = dialog.epd;
		for (var epdMethod : epd.impacts()) {
			var impacts = epd.getImpactSet(epdMethod).orElse(null);
			if (impacts == null || impacts.isEmpty())
				continue;
			var scopes = new HashSet<String>();
			impacts.each((_indicator, scopeSet) -> {
				for (var scope : scopeSet.scopes()) {
					scopes.add(scope);
				}
			});
			for (var scope : scopes) {
				sections.add(new ResultSection(dialog, epdMethod, scope));
			}
		}

		sections.sort((s1, s2) -> {
			var c = Strings.compare(s1.epdMethod, s2.epdMethod);
			return c != 0
				? c
				: Strings.compare(s1.epdScope, s2.epdScope);
		});

		return sections;
	}

	void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Result: " + result.name);
		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		var top = tk.createComposite(comp);
		UI.gridData(top, true, false);
		UI.gridLayout(top, 2, 10, 0);

		// name
		Controls.set(UI.formText(top, tk, M.Name), result.name, name -> {
			result.name = name;
			section.setText(result.name);
		});

		// EPD method and scope
		UI.formLabel(top, tk, "EPD LCIA Method");
		var methodLabel = UI.formLabel(top, tk, epdMethod);
		var _method = dialog.impactModel.getMethod(epdMethod);
		if (_method != null && _method.description() != null) {
			methodLabel.setToolTipText(_method.description());
		}
		UI.formLabel(top, tk, "EPD Scope");
		UI.formLabel(top, tk, epdScope);

		// mapped openLCA method
		var methodCombo = UI.formCombo(top, tk, "openLCA LCIA Method");
		var methods = new ImpactMethodDao(dialog.db).getAll();
		if (_method != null) {
			var selected = _method.matchMethod(methods);
			if (selected != null) {
				result.setup.withImpactMethod(selected);
			}
		}
		EntityCombo.of(methodCombo, methods)
			.select(result.setup.impactMethod())
			.onSelected(method -> {
				result.setup.withImpactMethod(method);
				// TODO: update the mapping table
			});

		// indicator mappings
		var mappingTable = Tables.createViewer(comp,
			/* 0 */ "EPD Indicator",
			/* 1 */ "Result",
			/* 2 */ "openLCA Unit",
			/* 3 */ "openLCA Indicator");
		Tables.bindColumnWidths(mappingTable, 0.35, 0.15, 0.15, 0.35);
		mappingTable.setLabelProvider(new MappingLabel());
		mappingTable.setInput(mappedValues);

	}

	private List<MappedValue> initMappings() {

		var epdImpacts = dialog.epd.getImpactSet(epdMethod).orElse(null);
		if (epdImpacts == null || epdImpacts.isEmpty())
			return Collections.emptyList();

		var values = new ArrayList<MappedValue>();
		epdImpacts.each((epdId, scopes) -> {
			var indicator = dialog.impactModel.getIndicator(epdId);
			var epdName = indicator != null ? indicator.name() : "";
			var result = scopes.get(epdScope).orElse(null);
			var epdUnit = result != null
				? result.unit
				: indicator != null ? indicator.unit() : "";
			var epdIndicator = new EpdIndicator(epdId, epdName, epdUnit);
			var value = new MappedValue(epdIndicator);
			value.value = result != null ? result.mean : 0;
			values.add(value);
		});
		values.sort(Comparator.comparing(v -> v.epdImpact.toString()));
		return values;
	}


	private static class MappedValue {
		final EpdIndicator epdImpact;
		double value;
		ImpactCategory mappedImpact;

		MappedValue(EpdIndicator epdImpact) {
			this.epdImpact = epdImpact;
		}
	}

	private record EpdIndicator(String id, String name, String unit) {
		@Override
		public String toString() {
			var s = id.toUpperCase();
			if (Strings.notEmpty(name)) {
				s += " - " + name;
			}
			if (Strings.notEmpty(unit)) {
				s += " [" + unit + "]";
			}
			return s;
		}
	}

	private static class MappingLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return switch (col) {
				case 0 -> Icon.BUILDING.get();
				case 2 -> Images.get(ModelType.UNIT);
				case 3 -> Images.get(ModelType.IMPACT_CATEGORY);
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof MappedValue value))
				return null;
			return switch (col) {
				case 0 -> value.epdImpact.toString();
				case 1 -> Numbers.format(value.value);
				case 2 -> value.mappedImpact != null
					? value.mappedImpact.referenceUnit
					: null;
				case 3 -> Labels.name(value.mappedImpact);
				default -> null;
			};
		}
	}
}
