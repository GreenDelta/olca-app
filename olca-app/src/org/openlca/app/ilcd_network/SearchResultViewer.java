package org.openlca.app.ilcd_network;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.openlca.app.Messages;

/**
 * The table viewer for search results.
 * 
 * @author Michael Srocka
 * 
 */
class SearchResultViewer extends TableViewer {

	static final int NAME_COLUMN = 0;
	static final int LOCATION_COLUMN = 1;
	static final int TIME_COLUMN = 2;
	static final int TYPE_COLUMN = 3;

	private String[] columnLabel = { Messages.Name, Messages.Location,
			Messages.Time, Messages.Type };
	private int[] columnWidth = { 200, 80, 80, 80 };

	/**
	 * Create a new viewer.
	 */
	public SearchResultViewer(Table table) {
		super(table);
		createColumns(); // must be done before label provider is set
		setLabelProvider(new SearchResultLabel());
		setContentProvider(new ArrayContentProvider());
	}

	/**
	 * Create the table columns.
	 */
	private void createColumns() {
		for (int col = 0; col < columnLabel.length; col++) {
			TableViewerColumn column = new TableViewerColumn(this, SWT.NONE);
			column.getColumn().setText(columnLabel[col]);
			column.getColumn().setWidth(columnWidth[col]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
		}
	}

}
