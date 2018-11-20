package org.openlca.app.util.viewers;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.openlca.util.Strings;

class LabelComparator<T> extends Comparator<T> {

	private final ITableLabelProvider provider;
	boolean asNumbers;

	LabelComparator(int column, ITableLabelProvider labelProvider) {
		super(column);
		this.provider = labelProvider;
	}

	@Override
	protected int compare(T e1, T e2) {
		String text1 = provider.getColumnText(e1, column);
		String text2 = provider.getColumnText(e2, column);
		if (!asNumbers)
			return Strings.compare(text1, text2);
		Double d1 = safeParse(text1);
		Double d2 = safeParse(text2);
		if (d1 == null && d2 == null)
			return 0;
		if (d1 == null || d2 == null)
			return d1 == null ? -1 : 1;
		return Double.compare(Double.parseDouble(text1), Double.parseDouble(text2));
	}

	private Double safeParse(String text) {
		if (text == null)
			return null;
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
