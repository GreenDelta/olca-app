package org.openlca.app.util.tables;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.openlca.util.Strings;

class LabelSorter<T> extends Sorter<T> {

	private final ITableLabelProvider provider;

	LabelSorter(int column, ITableLabelProvider labelProvider) {
		super(column);
		this.provider = labelProvider;
	}

	@Override
	protected int compare(Object e1, Object e2) {
		if (e1 == null && e2 == null)
			return 0;
		if (e1 == null || e2 == null)
			return e1 == null ? -1 : 1;
		String text1 = provider.getColumnText(e1, column);
		String text2 = provider.getColumnText(e2, column);
		return Strings.compare(text1, text2);
	}

}
