package org.openlca.app.collaboration.ui.viewers;

import java.util.Collection;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.collaboration.model.LibraryRestriction;
import org.openlca.app.collaboration.model.LibraryRestriction.RestrictionType;
import org.openlca.app.navigation.ModelTypeOrder;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.git.model.Reference;
import org.openlca.util.Strings;

public class LibraryRestrictionViewer extends AbstractTableViewer<LibraryRestriction> {

	public LibraryRestrictionViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TableViewer createViewer(Composite parent) {
		var viewer = super.createViewer(parent);
		Tables.bindColumnWidths(viewer, 0.7, 0.25, 0.05);
		viewer.setComparator(new LibraryComparator());
		return viewer;
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { M.DataSet, M.Library, "" };
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LibraryLabelProvider();
	}

	@Override
	public void setInput(Collection<LibraryRestriction> collection) {
		super.setInput(collection);
	}

	private class LibraryLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			var restriction = (LibraryRestriction) element;
			switch (column) {
			case 0:
				return Images.get(restriction.ref.type);
			case 2:
				return restriction.type == RestrictionType.FORBIDDEN
						? Icon.ERROR.get()
						: Icon.WARNING.get();
			default:
				return null;
			}
		}

		@Override
		public String getColumnText(Object element, int column) {
			var restriction = (LibraryRestriction) element;
			switch (column) {
			case 0:
				return restriction.ref.fullPath;
			case 1:
				return restriction.library;
			default:
				return null;
			}
		}

	}

	private class LibraryComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object o1, Object o2) {
			var l1 = (LibraryRestriction) o1;
			var l2 = (LibraryRestriction) o2;
			if (l1.type == RestrictionType.FORBIDDEN && l2.type == RestrictionType.WARNING)
				return -1;
			if (l1.type == RestrictionType.WARNING && l2.type == RestrictionType.FORBIDDEN)
				return 1;
			return compare(viewer, l1.ref, l2.ref);
		}

		private int compare(Viewer viewer, Reference r1, Reference r2) {
			var c = ModelTypeOrder.compare(r1.type, r2.type);
			if (c != 0)
				return c;
			return Strings.compare(r1.fullPath, r2.fullPath);
		}
	}
}
