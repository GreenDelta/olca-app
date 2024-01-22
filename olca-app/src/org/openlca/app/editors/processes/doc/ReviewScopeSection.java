package org.openlca.app.editors.processes.doc;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.doc.Review;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

class ReviewScopeSection {

	private final ProcessEditor editor;
	private final Supplier<Review> sync;

	ReviewScopeSection(ProcessEditor editor, Supplier<Review> sync) {
		this.editor = editor;
		this.sync = sync;
	}

	void render(Composite root, FormToolkit tk) {
		var section = UI.section(root, tk, "Review methods");
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(comp, "Scope", "Method");
		Tables.bindColumnWidths(table, 0.4, 0.6);
		table.setLabelProvider(new TableLabel());
		table.setInput(Item.allOf(sync.get()));
	}

	record Item(AtomicInteger index, String scope, String method) {

		static Item of(String scope, String method) {
			return new Item(new AtomicInteger(0), scope, method);
		}

		static List<Item> allOf(Review review) {
			if (review.scopes.isEmpty())
				return List.of();
			var list = new ArrayList<Item>();
			for (var scope : review.scopes) {
				for (var method : scope.methods) {
					list.add(Item.of(scope.name, method));
				}
			}
			list.sort((i1, i2) -> {
				int c = Strings.compare(i1.scope, i2.scope);
				return c == 0
						? Strings.compare(i1.method, i2.method)
						: c;
			});

			String last = null;
			int idx = 0;
			for (var i : list) {
				if (!Strings.nullOrEqual(last, i.scope)) {
					idx = 0;
					last = i.scope;
				}
				i.index.set(idx);
				idx++;
			}

			return list;
		}
	}

	private static class TableLabel extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			return switch (col) {
				case 0 -> item.index.get() == 0
						? item.scope
						: null;
				case 1 -> item.method;
				default -> null;
			};
		}
	}

}
