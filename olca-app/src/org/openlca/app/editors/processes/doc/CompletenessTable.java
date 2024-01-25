package org.openlca.app.editors.processes.doc;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.doc.ProcessDoc;
import org.openlca.ilcd.commons.FlowCompleteness;
import org.openlca.ilcd.commons.ImpactCategory;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class CompletenessTable {

	private final ProcessEditor editor;
	private final List<Item> items;

	CompletenessTable(ProcessEditor editor) {
		this.editor = editor;
		this.items = Item.readFrom(editor.getModel());
	}

	void render(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Completeness", 1);
		var table = Tables.createViewer(comp,
				"Aspect",
				labelOf(FlowCompleteness.ALL_RELEVANT_FLOWS_QUANTIFIED),
				labelOf(FlowCompleteness.RELEVANT_FLOWS_MISSING),
				labelOf(FlowCompleteness.TOPIC_NOT_RELEVANT),
				labelOf(FlowCompleteness.NO_STATEMENT));
		table.setInput(items);
		table.setLabelProvider(new TableLabel());
		Tables.bindColumnWidths(table, 0.4, 0.15, 0.15, 0.15, 0.15);

		var modifier = new ModifySupport<Item>(table);
		for (var v : FlowCompleteness.values()) {
			modifier.bind(labelOf(v), new CheckBoxCellModifier<>() {
				@Override
				protected boolean isChecked(Item item) {
					return item.value == v;
				}

				@Override
				protected void setChecked(Item item, boolean b) {
					var nextVal = b ? v : FlowCompleteness.NO_STATEMENT;
					if (item.value == nextVal)
						return;
					item.value = nextVal;
					Item.writeTo(items, editor.getModel());
					editor.setDirty();
				}
			});
		}
	}

	private static String labelOf(FlowCompleteness v) {
		if (v == null)
			return "?";
		return switch (v) {
			case ALL_RELEVANT_FLOWS_QUANTIFIED -> "All flows quantified";
			case RELEVANT_FLOWS_MISSING -> "Flows missing";
			case TOPIC_NOT_RELEVANT -> "Not relevant";
			case NO_STATEMENT -> "No statement";
		};
	}

	private static int columnOf(FlowCompleteness v) {
		if (v == null)
			return 4;
		return switch (v) {
			case ALL_RELEVANT_FLOWS_QUANTIFIED -> 1;
			case RELEVANT_FLOWS_MISSING -> 2;
			case TOPIC_NOT_RELEVANT -> 3;
			case NO_STATEMENT -> 4;
		};
	}

	private static class Item {

		final String aspect;
		FlowCompleteness value;

		Item(Map<String, String> c, String aspect) {
			this.aspect = aspect;
			value = FlowCompleteness.fromValue(c.get(aspect))
					.orElse(FlowCompleteness.NO_STATEMENT);
		}

		static List<Item> readFrom(Process process) {
			Map<String, String> c = process.documentation != null
					? process.documentation.flowCompleteness
					: Map.of();
			var items = new ArrayList<Item>();
			items.add(new Item(c, "Product model"));
			for (var impact : ImpactCategory.values()) {
				items.add(new Item(c, impact.value()));
			}
			return items;
		}

		static void writeTo(List<Item> items, Process process) {
			if (process.documentation == null) {
				process.documentation = new ProcessDoc();
			}
			var c = process.documentation.flowCompleteness;
			for (var item : items) {
				if (Strings.nullOrEmpty(item.aspect) || item.value == null)
					continue;
				c.put(item.aspect, item.value.value());
			}
		}
	}

	private static class TableLabel extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			if (col == 0)
				return Strings.nullOrEqual("Product model", item.aspect)
						? Images.get(FlowType.PRODUCT_FLOW)
						: Images.get(FlowType.ELEMENTARY_FLOW);
			return columnOf(item.value) == col
					? Icon.CHECK_TRUE.get()
					: Icon.CHECK_FALSE.get();
		}

		@Override
		public String getColumnText(Object obj, int col) {
			return col == 0 && obj instanceof Item item
					? item.aspect
					: null;
		}
	}

}
