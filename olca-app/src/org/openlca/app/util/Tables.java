package org.openlca.app.util;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * A helper class for creating tables, table viewers and related resources.
 */
public class Tables {

	/**
	 * Creates a default table viewer with the given properties. The properties
	 * are also used to create columns where each column label is the respective
	 * property of this column. The viewer is configured in the following way:
	 * <ul>
	 * <li>content provider = {@link ArrayContentProvider}
	 * <li>lines and header are visible
	 * <li>grid data with horizontal and vertical fill
	 * 
	 */
	public static TableViewer createViewer(Composite parent, String[] properties) {
		TableViewer viewer = new TableViewer(parent, SWT.BORDER
				| SWT.FULL_SELECTION);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setColumnProperties(properties);
		Table table = viewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		createColumns(table, properties);
		UI.gridData(table, true, true);
		return viewer;
	}

	public static void createColumns(Table table, String[] labels) {
		for (String label : labels) {
			TableColumn c = new TableColumn(table, SWT.NULL);
			c.setText(label);
		}
		for (TableColumn c : table.getColumns())
			c.pack();
	}

	public static void bindColumnWidths(TableViewer viewer, double... percents) {
		bindColumnWidths(viewer.getTable(), percents);
	}

	/**
	 * Binds the given percentage values (values between 0 and 1) to the column
	 * widths of the given table
	 */
	public static void bindColumnWidths(final Table table,
			final double... percents) {
		if (table == null || percents == null)
			return;
		table.addPaintListener(new PaintListener() {

			private boolean first = true;

			@Override
			public void paintControl(PaintEvent e) {
				if (!first)
					return;
				first = false;
				double width = table.getSize().x - 25;
				if (width < 50)
					return;
				TableColumn[] columns = table.getColumns();
				for (int i = 0; i < columns.length; i++) {
					if (i >= percents.length)
						break;
					double colWidth = percents[i] * width;
					columns[i].setWidth((int) colWidth);
				}

			}
		});

	}

}
