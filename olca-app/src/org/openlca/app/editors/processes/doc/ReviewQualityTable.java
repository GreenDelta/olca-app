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
import org.openlca.core.model.doc.Review;
import org.openlca.ilcd.commons.Quality;
import org.openlca.ilcd.commons.QualityIndicator;
import org.openlca.util.Strings;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

class ReviewQualityTable {

	private final ProcessEditor editor;
	private final Supplier<Review> sync;
	private final List<Item> items;

	ReviewQualityTable(ProcessEditor editor, Supplier<Review> sync) {
		this.editor = editor;
		this.sync = sync;
		this.items = Item.readFrom(sync.get());
	}

	void render(Composite root, FormToolkit tk) {
		var comp = UI.formSection(root, tk, "Quality assessment", 1);

		var qs = Quality.values();
		var props = new String[qs.length + 1];
		props[0] = "Aspect";
		for (int i = 0; i < qs.length; i++) {
			props[i + 1] = qs[i].value();
		}

		var table = Tables.createViewer(comp, props);
		table.setInput(items);
		table.setLabelProvider(new TableLabel());
		var widths = new double[props.length];
		widths[0] = 0.25;
		Arrays.fill(widths, 1, widths.length, 0.75 / ((double) qs.length));
		Tables.bindColumnWidths(table, widths);

		var modifier = new ModifySupport<Item>(table);
		for (var v : qs) {
			modifier.bind(v.value(), new CheckBoxCellModifier<>() {
				@Override
				protected boolean isChecked(Item item) {
					return item.value == v;
				}

				@Override
				protected void setChecked(Item item, boolean b) {
					var nextVal = b ? v : Quality.NOT_EVALUATED_UNKNOWN;
					if (item.value == nextVal)
						return;
					item.value = nextVal;
					var review = sync.get();
					review.assessment.put(item.aspect, nextVal.value());
					editor.setDirty();
				}
			});
		}
	}

	private static class Item {

		final String aspect;
		Quality value;

		Item(Map<String, String> aspects, String aspect) {
			this.aspect = aspect;
			value = Quality.fromValue(aspects.get(aspect))
					.orElse(null);
		}

		static List<Item> readFrom(Review review) {
			var a = review.assessment;
			return Arrays.stream(QualityIndicator.values())
					.map(qi -> new Item(a, qi.value()))
					.sorted((i1, i2) -> Strings.compare(i1.aspect, i2.aspect))
					.toList();
		}
	}

	private static class TableLabel extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col == 0 || !(obj instanceof Item item))
				return null;
			int idx = item.value != null
					? 1 + item.value.ordinal()
					: 1 + Quality.NOT_EVALUATED_UNKNOWN.ordinal();
			return idx == col
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

