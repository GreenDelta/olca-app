package org.openlca.app.collaboration.viewers;

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
import org.openlca.app.collaboration.model.Restriction;
import org.openlca.app.collaboration.model.Restriction.RestrictionType;
import org.openlca.app.navigation.ModelTypeOrder;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.util.Strings;

public class RestrictionViewer extends AbstractTableViewer<Restriction> {

	public RestrictionViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TableViewer createViewer(Composite parent) {
		var viewer = super.createViewer(parent);
		Tables.bindColumnWidths(viewer, 0.7, 0.25, 0.05);
		viewer.setComparator(new RestrictionComparator());
		return viewer;
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { M.DataSet, M.Restriction, "" };
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new RestrictionLabelProvider();
	}

	@Override
	public void setInput(Collection<Restriction> collection) {
		super.setInput(collection);
	}

	private class RestrictionLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			var restriction = (Restriction) element;
			switch (column) {
			case 0:
				return Images.get(restriction.modelType);
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
			var restriction = (Restriction) element;
			switch (column) {
			case 0:
				return restriction.path;
			case 1:
				return restriction.name;
			default:
				return null;
			}
		}

	}

	private class RestrictionComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object o1, Object o2) {
			var l1 = (Restriction) o1;
			var l2 = (Restriction) o2;
			if (l1.type == RestrictionType.FORBIDDEN && l2.type == RestrictionType.WARNING)
				return -1;
			if (l1.type == RestrictionType.WARNING && l2.type == RestrictionType.FORBIDDEN)
				return 1;
			return compare(viewer, l1, l2);
		}

		private int compare(Viewer viewer, Restriction l1, Restriction l2) {
			var c = ModelTypeOrder.compare(l1.modelType, l2.modelType);
			if (c != 0)
				return c;
			return Strings.compare(l1.path, l2.path);
		}
	}
}
