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

	private final List<Id> vars;
	private final TableViewer table;
	private Consumer<Id> selectionHandler;

	private Id selected;

	VarPanel(List<Var> vars, Composite comp, FormToolkit tk) {
		this.vars = vars == null
			? List.of()
			: vars.stream()
				.map(Var::name)
				.filter(Objects::nonNull)
				.sorted((i, j) -> Strings.compareIgnoreCase(i.label(), j.label()))
				.toList();

		createSearchText(comp, tk);
		table = createTable(comp);
		table.setInput(this.vars);
	}

	void onSelect(Consumer<Id> handler) {
		selectionHandler = handler;
	}

	private TableViewer createTable(Composite comp) {
		var table = new TableViewer(comp,
			SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		var gd = UI.gridData(table.getControl(), true, true);
		gd.heightHint = 1;
		gd.widthHint = 1;
		table.setContentProvider(ArrayContentProvider.getInstance());
		table.setLabelProvider(new IdLabel());

		table.addSelectionChangedListener(e -> {
			if (selectionHandler == null) return;
			selected = Viewers.getFirstSelected(table);
			selectionHandler.accept(selected);
		});
		return table;
	}

	private void createSearchText(Composite comp, FormToolkit tk) {
		var text = UI.text(comp, tk, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		UI.gridData(text, true, false);
		text.setMessage("Search");
		text.addModifyListener(e -> doSearch(text.getText()));
	}

	private void doSearch(String query) {
		if (Strings.isBlank(query)) {
			table.setInput(vars);
			return;
		}

		var q = query.strip().toLowerCase();
		if (selected != null && !matches(selected, q)) {
			selected = null;
			if (selectionHandler != null) {
				selectionHandler.accept(null);
			}
		}

		var filtered = vars.stream()
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
