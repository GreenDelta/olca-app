package org.openlca.core.editors.process;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.TableColumn;

class ExchangeSortListener implements SelectionListener {

	private ExchangeSorter sorter;
	private TableViewer viewer;

	public ExchangeSortListener(ExchangeSorter sorter, TableViewer viewer) {
		this.sorter = sorter;
		this.viewer = viewer;
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		Object source = e.getSource();
		if (!(source instanceof TableColumn))
			return;
		TableColumn c = (TableColumn) source;
		String newProp = c.getText();
		String oldProp = sorter.getSortProperty();
		if (newProp.equals(oldProp))
			sorter.setAscending(!sorter.isAscending());
		else
			sorter.setSortProperty(newProp);
		c.getParent().setSortColumn(c);
		c.getParent()
				.setSortDirection(sorter.isAscending() ? SWT.DOWN : SWT.UP);
		viewer.refresh();
	}
}