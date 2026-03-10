package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.commons.Strings;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Rate;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Stock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

class StockFlowPanel {

	private final SdModel model;
	private final Stock stock;

	StockFlowPanel(SdModel model, Stock stock) {
		this.model = Objects.requireNonNull(model);
		this.stock = Objects.requireNonNull(stock);
	}

	void render(Composite parent, FormToolkit tk, Runnable onChange) {
		UI.filler(parent, tk);

		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 2, 5, 0);
		UI.gridData(comp, true, false);

		UI.label(comp, tk, "Input flows");
		UI.label(comp, tk, "Output flows");

		new ListBox(comp, stock.inFlows(), onChange);
		new ListBox(comp, stock.outFlows(), onChange);
	}

	private List<Id> getNewFlowCandidates() {
		var used = new HashSet<>(stock.inFlows());
		used.addAll(stock.outFlows());
		var candidates = new ArrayList<Id>();
		for (var v : model.vars()) {
			if (!(v instanceof Rate rate)
				|| used.contains(rate.name())) {
				continue;
			}
			candidates.add(rate.name());
		}
		candidates.sort((i, j) -> Strings.compareIgnoreCase(i.label(), j.label()));
		return candidates;
	}

	private class ListBox {

		private final org.eclipse.swt.widgets.List widget;
		private final List<Id> flows;

		ListBox(Composite comp, List<Id> flows, Runnable onChange) {
			widget = new org.eclipse.swt.widgets.List(
				comp, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
			this.flows = flows;

			var gd = UI.gridData(widget, true, false);
			gd.widthHint = 1;
			gd.heightHint = 60;

			var menu = new Menu(widget);
			widget.setMenu(menu);

			var addItem = new MenuItem(menu, SWT.NONE);
			addItem.setText(M.Add);
			addItem.setImage(Icon.ADD.get());
			Controls.onSelect(addItem, $ -> {
				var candidates = getNewFlowCandidates();
				FlowSelector.open(candidates, selected -> {
					flows.addAll(selected);
					updateItems();
					onChange.run();
				});
			});

			var delItem = new MenuItem(menu, SWT.NONE);
			delItem.setText(M.Remove);
			delItem.setImage(Icon.DELETE.get());
			Controls.onSelect(delItem, $ -> {
				var idx = widget.getSelectionIndex();
				if (idx < 0) return;
				var item = widget.getItem(idx);
				if (item == null) return;
				var flow = Id.of(item);
				flows.removeIf(flow::equals);
				updateItems();
				onChange.run();
			});

			updateItems();
		}

		private void updateItems() {
			var items = flows.stream()
				.map(Id::label)
				.sorted()
				.toArray(String[]::new);
			widget.setItems(items);
		}
	}

	private static class FlowSelector extends FormDialog {

		private final List<Id> candidates;
		private final Consumer<List<Id>> onResult;
		private ListViewer list;

		static void open(List<Id> candidates, Consumer<List<Id>> onResult) {
			if (candidates == null || candidates.isEmpty()) return;
			new FlowSelector(candidates, onResult).open();
		}

		private FlowSelector(List<Id> candidates, Consumer<List<Id>> onResult) {
			super(UI.shell());
			this.candidates = candidates;
			this.onResult = onResult;
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Select flow(s)");
		}

		@Override
		protected Point getInitialSize() {
			return new Point(400, 500);
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var tk = mForm.getToolkit();
			var body = UI.dialogBody(mForm.getForm(), tk);
			UI.gridLayout(body, 1);

			var filter = UI.text(
				body, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
			UI.gridData(filter, true, false);
			filter.setMessage(M.Filter);
			filter.addModifyListener(e -> {
				var ids = applyFilter(filter.getText());
				list.setInput(ids);
			});

			list = new ListViewer(body, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
			UI.gridData(list.getControl(), true, true);
			list.setContentProvider(ArrayContentProvider.getInstance());
			list.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object o) {
					return o instanceof Id id
						? id.label()
						: super.getText(o);
				}
			});
			list.setInput(candidates);
		}

		private List<Id> applyFilter(String query) {
			if (Strings.isBlank(query)) {
				return candidates;
			}
			var q = query.trim().toLowerCase();
			return candidates.stream()
				.filter(id -> id.label().toLowerCase().contains(q))
				.toList();
		}

		@Override
		protected void okPressed() {
			List<Id> selection = Viewers.getAllSelected(list);
			if (!selection.isEmpty()) {
				onResult.accept(selection);
			}
			super.okPressed();
		}
	}
}
