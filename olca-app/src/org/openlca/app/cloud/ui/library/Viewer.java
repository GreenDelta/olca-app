package org.openlca.app.cloud.ui.library;

import java.util.Collection;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.cloud.model.LibraryRestriction;

class Viewer extends AbstractTableViewer<LibraryRestriction> {

	Viewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TableViewer createViewer(Composite parent) {
		TableViewer viewer = super.createViewer(parent);
		Tables.bindColumnWidths(viewer, 0.7, 0.25, 0.05);
		viewer.setComparator(new Comparator());
		return viewer;
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { M.DataSet, M.Library, ""};
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	@Override
	public void setInput(Collection<LibraryRestriction> collection) {
		super.setInput(collection);
	}

}
