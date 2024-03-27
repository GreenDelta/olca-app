package org.openlca.app.editors.processes.doc;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.doc.Review;
import org.openlca.ilcd.processes.ReviewMethod;
import org.openlca.ilcd.processes.ReviewScope;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

		if (!editor.isEditable())
			return;

		var add = Actions.onAdd(() -> {
			var item = ItemDialog.show().orElse(null);
			if (item == null)
				return;
			var rev = sync.get();
			if (item.addTo(rev)) {
				table.setInput(Item.allOf(rev));
				editor.setDirty();
			}
		});

		var del = Actions.onRemove(() -> {
			Item item = Viewers.getFirstSelected(table);
			if (item == null)
				return;
			var rev = sync.get();
			if (item.removeFrom(rev)) {
				table.setInput(Item.allOf(rev));
				editor.setDirty();
			}
		});

		Actions.bind(section, add, del);
		Actions.bind(table, add, del);
	}

	record Item(AtomicInteger index, String scope, String method) {

		static Item of(String scope, String method) {
			return new Item(new AtomicInteger(0), scope, method);
		}

		static List<Item> allOf(Review review) {
			if (review.scopes.isEmpty())
				return List.of();
			var list = new ArrayList<Item>();
			for (var scope : review.scopes.values()) {
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

		boolean addTo(Review rev) {
			for (var scope : rev.scopes.values()) {
				if (!Strings.nullOrEqual(this.scope, scope.name))
					continue;
				if (scope.methods.contains(method))
					return false;
				scope.methods.add(method);
				return true;
			}

			var scope = new org.openlca.core.model.doc.ReviewScope(this.scope);
			scope.methods.add(this.method);
			rev.scopes.put(scope);
			return true;
		}

		boolean removeFrom(Review rev) {			
			var scope = rev.scopes.get(this.scope);
			return scope != null && scope.methods.remove(method);
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

	private static class ItemDialog extends FormDialog {

		private String scope;
		private String method;

		static Optional<Item> show() {
			var dialog = new ItemDialog();
			return dialog.open() == OK
					? Optional.of(Item.of(dialog.scope, dialog.method))
					: Optional.empty();
		}

		private ItemDialog() {
			super(UI.shell());
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setText("Add a review method");
		}

		@Override
		protected Point getInitialSize() {
			return new Point(550, 250);
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var tk = mForm.getToolkit();
			var body = UI.dialogBody(mForm.getForm(), tk);
			UI.gridLayout(body, 2);

			var scopeCombo = UI.labeledCombo(body, tk, "Scope");
			var scopes = ReviewScope.values();
			var scopeItems = new String[scopes.length];
			for (int i = 0; i < scopes.length; i++) {
				scopeItems[i] = scopes[i].value();
			}
			Arrays.sort(scopeItems);
			scopeCombo.setItems(scopeItems);
			scopeCombo.select(0);
			this.scope = scopeItems[0];
			Controls.onSelect(scopeCombo, $ -> {
				int i = scopeCombo.getSelectionIndex();
				this.scope = scopeItems[i];
			});

			var methodCombo = UI.labeledCombo(body, tk, "Method");
			var methods = ReviewMethod.values();
			var methodItems = new String[methods.length];
			for (int i = 0; i < methods.length; i++) {
				methodItems[i] = methods[i].value();
			}
			Arrays.sort(methodItems);
			methodCombo.setItems(methodItems);
			methodCombo.select(0);
			this.method = methodItems[0];
			Controls.onSelect(methodCombo, $ -> {
				int i = methodCombo.getSelectionIndex();
				this.method = methodItems[i];
			});
		}
	}
}
