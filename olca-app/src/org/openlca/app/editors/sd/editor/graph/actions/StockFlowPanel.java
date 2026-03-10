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
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Stock;

class StockFlowPanel {

	private final Stock stock;

	StockFlowPanel(Composite parent, FormToolkit tk, Stock stock) {
		this.stock = stock;

		UI.filler(parent, tk);

		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 2, 5, 0);
		UI.gridData(comp, true, false);

		UI.label(comp, tk, "Input flows");
		UI.label(comp, tk, "Output flows");

		ListBox.create(comp, stock.inFlows());
		ListBox.create(comp, stock.outFlows());

	}

	private record ListBox(
		List widget, java.util.List<Id> data
	) {

		static void create(Composite comp, java.util.List<Id> data) {

			var widget = new List(comp, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
			var box = new ListBox(widget, data);
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
			});

			var delItem = new MenuItem(menu, SWT.NONE);
			delItem.setText(M.Remove);
			delItem.setImage(Icon.DELETE.get());
			Controls.onSelect(delItem, $ -> {
				var idx = widget.getSelectionIndex();
				if (idx < 0) return;
				var item = widget.getItem(idx);
				if (item != null) {
					var flow = Id.of(item);
					data.removeIf(flow::equals);
					box.updateItems();
				}
			});

			box.updateItems();
		}

		private void updateItems() {
			var items = data.stream()
				.map(Id::label)
				.sorted()
				.toArray(String[]::new);
			widget.setItems(items);
		}
	}

}
