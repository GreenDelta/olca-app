package org.openlca.app.viewer;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class AbstractTableViewer<T> extends AbstractViewer<T> {

	protected AbstractTableViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		TableViewer viewer = new TableViewer(parent, SWT.BORDER
				| SWT.FULL_SELECTION | SWT.MULTI);

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(getLabelProvider());
		viewer.setSorter(getSorter());

		Table table = viewer.getTable();
		String[] columnHeaders = getColumnHeaders();
		if (!useColumnHeaders()) {
			table.setLinesVisible(false);
			table.setHeaderVisible(false);
		} else {
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			for (String p : columnHeaders)
				new TableColumn(table, SWT.NULL).setText(p);
			for (TableColumn c : table.getColumns())
				c.pack();
		}
		if (useColumnHeaders())
			viewer.setColumnProperties(columnHeaders);
		return viewer;
	}

	/**
	 * Subclasses may override this for support of column headers for the table
	 * combo, if null or empty array is returned, the headers are not visible
	 * and the combo behaves like a standard combo
	 */
	protected String[] getColumnHeaders() {
		return null;
	}

	private boolean useColumnHeaders() {
		return getColumnHeaders() != null && getColumnHeaders().length > 0;
	}

	public void addDoubleClickListener(IDoubleClickListener listener) {
		getViewer().addDoubleClickListener(listener);
	}
	
}
