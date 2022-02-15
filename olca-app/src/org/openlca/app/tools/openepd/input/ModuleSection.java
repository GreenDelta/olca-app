package org.openlca.app.tools.openepd.input;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.EntityCombo;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.openepd.model.Ec3ImpactModel;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.DoubleCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ImpactResult;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;
import org.openlca.util.Strings;

class ModuleSection {

	private final ImportDialog dialog;
	private final String epdMethod;
	private final String epdScope;
	private final Result result;
	private final List<MappedValue> mappedValues;
	private TableViewer mappingTable;
	private Consumer<ModuleSection> onDeleted;

	private ModuleSection(
		ImportDialog dialog, String epdMethod, String epdScope) {
		this.dialog = dialog;
		this.epdMethod = epdMethod;
		this.epdScope = epdScope;
		this.result = Result.of(
			dialog.epd.productName + " - " + epdScope + " - " + epdMethod);
		mappedValues = initMappings();
	}

	public void onDeleted(Consumer<ModuleSection> fn) {
		this.onDeleted = fn;
	}

	/**
	 * Initializes a result section for each method and scope pair that can
	 * be found in the EPD of the given import dialog.
	 */
	static List<ModuleSection> initAllOf(ImportDialog dialog) {
		if (dialog == null)
			return Collections.emptyList();

		var sections = new ArrayList<ModuleSection>();
		var epd = dialog.epd;
		for (var epdResult : epd.impactResults) {
			var scopes = new HashSet<String>();
			for (var indicatorResult : epdResult.indicatorResults()) {
				for (var scopeValue : indicatorResult.values()) {
					scopes.add(scopeValue.scope());
				}
			}
			for (var scope : scopes) {
				sections.add(new ModuleSection(dialog, epdResult.method(), scope));
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

	EpdModule createModule() {
		var result = this.result.copy();
		result.refId = UUID.randomUUID().toString();
		for (var mapping : mappedValues) {
			if (mapping.mappedImpact == null)
				continue;
			var impact = new ImpactResult();
			impact.amount = mapping.value;
			impact.indicator = mapping.mappedImpact;
			result.impactResults.add(impact);
		}
		return EpdModule.of(epdScope, result);
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
		var methodCombo = UI.formCombo(top, tk, "Mapped LCIA Method");
		var methods = new ImpactMethodDao(dialog.db).getAll();
		if (_method != null) {
			var selected = _method.matchMethod(methods);
			if (selected != null) {
				result.impactMethod = selected;
				map(selected);
			}
		}
		EntityCombo.of(methodCombo, methods)
			.select(result.impactMethod)
			.onSelected(method -> {
				result.impactMethod = method;
				map(method);
			});

		// indicator mappings
		mappingTable = Tables.createViewer(comp,
			/* 0 */ "EPD Indicator",
			/* 1 */ "Result",
			/* 2 */ "Unit",
			/* 3 */ "openLCA Indicator");
		Tables.bindColumnWidths(mappingTable, 0.35, 0.15, 0.15, 0.35);
		mappingTable.setLabelProvider(new MappingLabel());
		mappingTable.setInput(mappedValues);
		new ModifySupport<MappedValue>(mappingTable)
			.bind("openLCA Indicator", new ImpactModifier(result))
			.bind("Result", new ValueModifier());

		// add the removal button
		var onRemove = Actions.onRemove(() -> {
			if (onDeleted != null) {
				onDeleted.accept(this);
			}
			section.dispose();
			body.layout();
		});
		Actions.bind(section, onRemove);
	}

	private List<MappedValue> initMappings() {

		var impactResult = dialog.epd.impactResults.stream()
			.filter(r -> Objects.equals(r.method(), epdMethod))
			.findAny()
			.orElse(null);
		if (impactResult == null || impactResult.indicatorResults().isEmpty())
			return Collections.emptyList();

		var values = new ArrayList<MappedValue>();
		for (var indicatorResult : impactResult.indicatorResults()) {
			var indicatorId = indicatorResult.indicator();
			for (var scopeValue : indicatorResult.values()) {
				if (!Objects.equals(scopeValue.scope(), epdScope))
					continue;
				var indicator = dialog.impactModel.getIndicator(indicatorId);
				var epdName = indicator != null
					? indicator.name()
					: "";
				var result = scopeValue.value();
				var epdUnit = result != null
					? result.unit()
					: indicator != null ? indicator.unit() : "";
				var epdIndicator = new EpdIndicator(indicatorId, epdName, epdUnit);
				var value = new MappedValue(epdIndicator);
				value.value = result != null ? result.mean() : 0;
				values.add(value);
			}
		}
		values.sort(Comparator.comparing(v -> v.epdImpact.toString()));
		return values;
	}

	private void map(ImpactMethod method) {
		var queue = new ArrayDeque<>(method.impactCategories);
		var model = dialog.impactModel;
		var bindings = new HashMap<String, MapScore>();

		while (!queue.isEmpty()) {

			// find the best score for the next indicator
			var next = queue.poll();
			String bestMatch = null;
			int bestScore = 0;
			for (var mapping : mappedValues) {

				// calculate a score
				var epdId = mapping.epdImpact.id;
				var epdIndicator = model.getIndicator(epdId);
				if (epdIndicator == null)
					continue;
				var nextScore = Ec3ImpactModel.mapScore(
					Labels.name(next), epdIndicator.keywords());
				if (nextScore <= bestScore)
					continue;

				// check a possible binding
				var binding = bindings.get(epdId);
				if (binding != null && binding.score >= nextScore)
					continue;

				// found a better match
				bestMatch = epdId;
				bestScore = nextScore;

			} // for


			if (bestMatch == null)
				continue;
			// remove a possible binding
			var binding = bindings.get(bestMatch);
			if (binding != null) {
				queue.add(binding.impact);
			}
			bindings.put(bestMatch, new MapScore(next, bestScore));

		} // while

		// update the mappings
		for (var mv : mappedValues) {
			var binding = bindings.get(mv.epdImpact.id);
			mv.mappedImpact = binding != null
				? binding.impact
				: null;
		}
		if (mappingTable != null) {
			mappingTable.refresh();
		}

	}

	private record MapScore(ImpactCategory impact, double score) {
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

	private static class ImpactModifier extends
		ComboBoxCellModifier<MappedValue, ImpactCategory> {

		private final Result result;

		ImpactModifier(Result result) {
			this.result = result;
		}

		@Override
		protected ImpactCategory[] getItems(MappedValue mv) {
			var empty = new ImpactCategory[0];
			if (result == null || result.impactMethod == null)
				return empty;

			var method = result.impactMethod;
			var impacts = method.impactCategories.toArray(empty);
			Arrays.sort(impacts,
				(i1, i2) -> Strings.compare(Labels.name(i1), Labels.name(i2)));
			return impacts;
		}

		@Override
		protected ImpactCategory getItem(MappedValue mv) {
			return mv.mappedImpact;
		}

		@Override
		protected String getText(ImpactCategory impact) {
			return Labels.name(impact);
		}

		@Override
		protected void setItem(MappedValue mv, ImpactCategory impact) {
			mv.mappedImpact = impact;
		}
	}

	private static class ValueModifier extends DoubleCellModifier<MappedValue> {

		@Override
		public Double getDouble(MappedValue mv) {
			return mv.value;
		}

		@Override
		public void setDouble(MappedValue mv, Double value) {
			mv.value = value == null
				? 0
				: value;
		}
	}
}
