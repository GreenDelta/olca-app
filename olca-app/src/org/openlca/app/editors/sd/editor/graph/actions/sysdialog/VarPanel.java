package org.openlca.app.editors.sd.editor.graph.actions.sysdialog;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.commons.Strings;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Var;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

class VarPanel {

	private final List<Id> all;
	private Id selected;
	private Consumer<Id> onSelection = _id -> {
	};

	private final TableViewer table;

	VarPanel(List<Var> vars, Composite comp, FormToolkit tk) {
		all = vars == null
			? List.of()
			: vars.stream()
				.map(Var::name)
				.filter(Objects::nonNull)
				.sorted((i, j) -> Strings.compareIgnoreCase(i.label(), j.label()))
				.toList();

		var filter = UI.text(comp, tk,
			SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		UI.gridData(filter, true, false);
		filter.setMessage("Search");
		filter.addModifyListener(e -> applyFilter(filter.getText()));

		table = new TableViewer(comp,
			SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		var gd = UI.gridData(table.getControl(), true, true);
		gd.heightHint = 1;
		gd.widthHint = 1;
		table.setContentProvider(ArrayContentProvider.getInstance());
		table.setLabelProvider(new IdLabel());
		table.setInput(all);
		table.addSelectionChangedListener(e -> {
			selected = Viewers.getFirstSelected(table);
			onSelection.accept(selected);
		});
	}

	Id selected() {
		return selected;
	}

	TableViewer table() {
		return table;
	}

	void onSelection(Consumer<Id> handler) {
		onSelection = handler != null
			? handler
			: _id -> {
			};
	}

	private void applyFilter(String query) {
		if (Strings.isBlank(query)) {
			table.setInput(all);
			return;
		}

		var q = query.strip().toLowerCase();
		if (selected != null && !matches(selected, q)) {
			selected = null;
			onSelection.accept(null);
		}

		var filtered = all.stream()
			.filter(id -> matches(id, q))
			.toList();
		table.setInput(filtered);
	}

	private boolean matches(Id id, String query) {
		return id != null
			&& id.label() != null
			&& id.label().toLowerCase().contains(query);
	}

	private static class IdLabel extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return Icon.FORMULA.get();
		}

		@Override
		public String getColumnText(Object obj, int col) {
			return obj instanceof Id id
				? id.label()
				: null;
		}
	}

}
