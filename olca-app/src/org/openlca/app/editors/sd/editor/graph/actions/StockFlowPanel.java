package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Input;
import org.openlca.app.util.UI;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Stock;

class StockFlowPanel {

	private final List inList;
	private final List outList;
	private final Stock stock;

	StockFlowPanel(Composite parent, FormToolkit tk, Stock stock) {
		this.stock = stock;

		UI.filler(parent, tk);

		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 2);
		UI.gridData(comp, true, false);

		createHeader(comp, tk, "Input flows");
		createHeader(comp, tk, "Output flows");

		inList = new List(comp, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
		UI.gridData(inList, true, false).heightHint = 40;
		updateList(inList, stock.inFlows());

		outList = new List(comp, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
		UI.gridData(outList, true, false).heightHint = 40;
		updateList(outList, stock.inFlows());

		createActions(comp, tk, inList, stock.inFlows());
		createActions(comp, tk, outList, stock.outFlows());
	}

	private void createHeader(Composite comp, FormToolkit tk, String text) {
		var label = tk.createLabel(comp, text, SWT.NONE);
		UI.gridData(label, true, false);
	}

	private void createActions(Composite comp, FormToolkit tk, List list, java.util.List<Id> data) {
		var bar = UI.composite(comp, tk);
		UI.gridLayout(bar, 2, 0, 0);
		UI.gridData(bar, true, false);

		var add = tk.createButton(bar, null, SWT.PUSH);
		add.setImage(Icon.ADD.get());
		add.setToolTipText(M.Add);
		Controls.onSelect(add, $ -> {
			var name = Input.promptString("Add flow", "Flow name", "");
			if (name != null && !name.isBlank()) {
				var id = Id.of(name);
				if (!data.contains(id)) {
					data.add(id);
					updateList(list, data);
				}
			}
		});

		var remove = tk.createButton(bar, null, SWT.PUSH);
		remove.setImage(Icon.DELETE.get());
		remove.setToolTipText(M.Remove);
		Controls.onSelect(remove, $ -> {
			int idx = list.getSelectionIndex();
			if (idx >= 0) {
				data.remove(idx);
				updateList(list, data);
			}
		});
	}

	private void updateList(List list, java.util.List<Id> data) {
		list.setItems(data.stream().map(Id::label).toArray(String[]::new));
	}
}
