package org.openlca.app.cloud.ui.library;

import java.util.Collection;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.cloud.model.data.Dataset;

class LibraryResultViewer extends AbstractTableViewer<Entry<Dataset, String>> {

	LibraryResultViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TableViewer createViewer(Composite parent) {
		TableViewer viewer = super.createViewer(parent);
		Tables.bindColumnWidths(viewer, 0.7, 0.3);
		viewer.setComparator(new Comparator());
		return viewer;
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { M.DataSet, M.Library };
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	@Override
	public void setInput(Collection<Entry<Dataset, String>> collection) {
		super.setInput(collection);
	}

}
