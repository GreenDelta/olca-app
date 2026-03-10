package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Rate;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Stock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

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

	private java.util.List<Id> getNewFlowCandidates() {
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

		private final List widget;
		private final java.util.List<Id> flows;

		ListBox(Composite comp, java.util.List<Id> flows, Runnable onChange) {
			widget = new List(comp, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
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
				// TODO -> open a dialog.
				onChange.run();
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
}
