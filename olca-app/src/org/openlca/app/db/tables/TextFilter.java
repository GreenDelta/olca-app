package org.openlca.app.db.tables;

import java.util.Objects;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Text;
import org.openlca.util.Strings;


class TextFilter extends ViewerFilter {

	private final TableViewer table;
	private final int columns;
	private String filter;
	private ITableLabelProvider label;

	private TextFilter(TableViewer table, Text text) {
		this.table = Objects.requireNonNull(table);
		this.columns = table.getTable().getColumnCount();
		text.addModifyListener($ -> {
			filter = text.getText().trim().toLowerCase();
			table.refresh();
		});
	}

	static TextFilter on(TableViewer table, Text text) {
		var filter = new TextFilter(table, text);
		table.setFilters(filter);
		return filter;
	}

	@Override
	public boolean select(Viewer viewer, Object parent, Object elem) {
		if (Strings.nullOrEmpty(filter))
			return true;
		if (label == null) {
			var tableLabel = table.getLabelProvider();
			if (!(tableLabel instanceof ITableLabelProvider))
				return true;
			label = (ITableLabelProvider) tableLabel;
		}
		for (int i = 0; i < columns; i++) {
			var text = label.getColumnText(elem, i);
			if (text == null)
				continue;
			if (text.toLowerCase().contains(filter))
				return true;
		}
		return false;
	}


}
