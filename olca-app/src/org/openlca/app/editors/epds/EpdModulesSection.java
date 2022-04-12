package org.openlca.app.editors.epds;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

import java.util.List;
import java.util.Objects;

record EpdModulesSection(EpdEditor editor) {

	void render(Composite body, FormToolkit tk) {

		// create the table
		var section = UI.section(body, tk, "Modules");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk);
		var table = Tables.createViewer(comp,
			"Module",
			"Result",
			"LCIA Method",
			"Quantitative reference");
		table.setLabelProvider(new LabelProvider());
		Tables.bindColumnWidths(table, 0.25, 0.25, 0.25, 0.25);

		// bind actions
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
			var copy = mod.copy();
			if (EpdModuleDialog.edit(editor.getModel(), copy)) {
				mod.name = copy.name;
				mod.result = copy.result;
				table.refresh(true);
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
			"Open result", Icon.FOLDER_OPEN.descriptor(), () -> {
				var mod = selectedModuleOf(table);
				if (mod != null && mod.result != null) {
					App.open(mod.result);
				}
			});

		Actions.bind(section, onAdd, onDelete);
		Actions.bind(table, onAdd, onEdit, onOpenResult, onDelete);
		Tables.onDoubleClick(table, $ -> onEdit.run());

		// fill the table
		var modules = modules();
		modules.sort((m1, m2) -> Strings.compare(m1.name, m2.name));
		table.setInput(modules);
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

	private static class LabelProvider extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col == 0)
				return Images.get(ModelType.RESULT);
			return null;
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
				case 3 -> {
					if (module.result == null
						|| module.result.referenceFlow == null)
						yield null;
					var refFlow = module.result.referenceFlow;
					yield Numbers.format(refFlow.amount, 2)
						+ " " + Labels.name(refFlow.unit)
						+ " " + Labels.name(refFlow.flow);
				}
				default -> null;
			};
		}
	}

}
