package org.openlca.app.editors.epds;

import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.math.ReferenceAmount;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

class EpdModulesSection {

	private final EpdEditor editor;
	private double lastRefAmount;

	EpdModulesSection(EpdEditor editor) {
		this.editor = editor;
	}

	void render(Composite body, FormToolkit tk) {

		// create the table
		var section = UI.section(body, tk, M.Modules);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk);
		var table = Tables.createViewer(comp,
				M.Module,
				M.Result,
				M.LciaMethod,
				M.ResultMultiplier,
				M.ReferenceFlow);
		table.setLabelProvider(new LabelProvider());
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);

		// bind actions
		if (editor.isEditable()) {
			bindActions(section, table);
		}
		lastRefAmount = currentRefAmount();
		editor.onEvent("amount.changed", () -> updateMultipliers(table));
		editor.onEvent("unit.changed", () -> updateMultipliers(table));

		// fill the table
		var modules = modules();
		modules.sort((mi, mj) -> Strings.compareIgnoreCase(mi.name, mj.name));
		table.setInput(modules);
	}

	private void bindActions(Section section, TableViewer table) {
		var onAdd = Actions.onAdd(
			() -> EpdModuleDialog.createNew(editor.getModel())
				.ifPresent(module -> {
					var mods = modules();
					mods.add(module);
					table.setInput(mods);
					editor.setDirty();
				}));

		var onEdit = Actions.onEdit(() -> {
			var mod = selectedModuleOf(table);
			if (mod == null) {
				onAdd.run();
				return;
			}
			if (EpdModuleDialog.edit(editor.getModel(), mod)) {
				table.setInput(modules());
				editor.setDirty();
			}
		});

		var onDelete = Actions.onRemove(() -> {
			var selected = selectedModulesOf(table);
			if (selected.isEmpty())
				return;
			var mods = modules();
			mods.removeAll(selected);
			table.setInput(mods);
			editor.setDirty();
		});

		var onOpenResult = Actions.create(
			M.OpenResult, Icon.FOLDER_OPEN.descriptor(), () -> {
				var mod = selectedModuleOf(table);
				if (mod != null && mod.result != null) {
					App.open(mod.result);
				}
			});

		Actions.bind(section, onAdd, onDelete);
		Actions.bind(table, onAdd, onEdit, onOpenResult, onDelete);
		Tables.onDoubleClick(table, $ -> onEdit.run());
	}

	private List<EpdModule> modules() {
		return editor.getModel().modules;
	}

	private EpdModule selectedModuleOf(TableViewer table) {
		var obj = Viewers.getFirstSelected(table);
		return obj instanceof EpdModule mod
			? syncedModuleOf(mod)
			: null;
	}

	private List<EpdModule> selectedModulesOf(TableViewer table) {
		return Viewers.getAllSelected(table)
			.stream()
			.filter(it -> it instanceof EpdModule)
			.map(EpdModule.class::cast)
			.map(this::syncedModuleOf)
			.filter(Objects::nonNull)
			.toList();
	}

	/**
	 * Get the JPA synced version of the module.
	 */
	private EpdModule syncedModuleOf(EpdModule mod) {
		for (var synced : modules()) {
			if (mod.id != synced.id)
				continue;
			if (mod.id != 0 || mod == synced)
				return synced;
		}
		return null;
	}

	private void updateMultipliers(TableViewer table) {
		var mods = modules();
		if (mods.isEmpty())
			return;
		double refAmount = currentRefAmount();
		double epsilon = 1e-10;
		if (refAmount < epsilon)
			return;
		if (lastRefAmount < epsilon) {
			lastRefAmount = refAmount;
			return;
		}
		double f = refAmount / lastRefAmount;
		if (Math.abs(1 - f) < epsilon)
			return;

		for (var mod : mods) {
			mod.multiplier *= f;
		}
		lastRefAmount = refAmount;
		table.setInput(mods);
	}

	private double currentRefAmount() {
		var product = editor.getModel().product;
		if (product == null
			|| product.flow == null
			|| product.property == null
			|| product.unit == null)
			return 0;
		var prop = product.flow.getFactor(product.property);
		return Math.abs(ReferenceAmount.get(product.amount, product.unit, prop));
	}

	private static class LabelProvider extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0
				? Images.get(ModelType.RESULT)
				: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof EpdModule module))
				return null;
			return switch (col) {
				case 0 -> module.name;
				case 1 -> Labels.name(module.result);
				case 2 -> module.result != null
					? Labels.name(module.result.impactMethod)
					: null;
				case 3 -> Double.toString(module.multiplier);
				case 4 -> qRef(module);
				default -> null;
			};
		}

		private String qRef(EpdModule module) {
			if (module.result == null)
				return "";
			var refFlow = module.result.referenceFlow;
			if (refFlow == null
				|| refFlow.flow == null
				|| refFlow.unit == null)
				return "";
			var amount = module.multiplier * refFlow.amount;
			return String.format(
				"%.2f %s - %s", amount, refFlow.unit.name, refFlow.flow.name);
		}
	}

}
