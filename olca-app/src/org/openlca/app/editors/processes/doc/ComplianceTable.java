package org.openlca.app.editors.processes.doc;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.model.doc.ComplianceDeclaration;
import org.openlca.ilcd.commons.Compliance;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

class ComplianceTable {

	private final ProcessEditor editor;
	private final Supplier<ComplianceDeclaration> sync;
	private final List<Item> items;

	ComplianceTable(ProcessEditor editor, Supplier<ComplianceDeclaration> sync) {
		this.editor = editor;
		this.sync = sync;
		this.items = Item.readFrom(sync.get());
	}

	void render(Composite root, FormToolkit tk) {
		var comp = UI.formSection(root, tk, "Compliance details", 1);
		var table = Tables.createViewer(
				comp,
				"Aspect",
				Compliance.FULLY_COMPLIANT.value(),
				Compliance.NOT_COMPLIANT.value(),
				Compliance.NOT_DEFINED.value());
		table.setInput(items);
		table.setLabelProvider(new TableLabel());
		Tables.bindColumnWidths(table, 0.25, 0.25, 0.25, 0.25);

		var modifier = new ModifySupport<Item>(table);
		for (var v : Compliance.values()) {
			modifier.bind(v.value(), new CheckBoxCellModifier<>() {
				@Override
				protected boolean isChecked(Item item) {
					return item.value == v;
				}

				@Override
				protected void setChecked(Item item, boolean b) {
					var nextVal = b ? v : Compliance.NOT_DEFINED;
					if (item.value == nextVal)
						return;
					item.value = nextVal;
					var dec = sync.get();
					dec.aspects.put(item.aspect, item.value.value());
					editor.setDirty();
				}
			});
		}
	}

	private static int columnOf(Compliance c) {
		if (c == null)
			return 3;
		return switch (c) {
			case FULLY_COMPLIANT -> 1;
			case NOT_COMPLIANT -> 2;
			case NOT_DEFINED -> 3;
		};
	}

	private static class Item {

		final String aspect;
		Compliance value;

		Item(Map<String, String> aspects, String aspect) {
			this.aspect = aspect;
			value = Compliance.fromValue(aspects.get(aspect))
					.orElse(Compliance.NOT_DEFINED);
		}

		static List<Item> readFrom(ComplianceDeclaration dec) {
			return Stream.of(
							"Overall compliance",
							"Nomenclature compliance",
							"Methodological compliance",
							"Review compliance",
							"Documentation compliance",
							"Quality compliance")
					.map(a -> new Item(dec.aspects, a))
					.toList();
		}
	}

	private static class TableLabel extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col == 0 || !(obj instanceof Item item))
				return null;
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

