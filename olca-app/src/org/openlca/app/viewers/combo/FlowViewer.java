package org.openlca.app.viewers.combo;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Category;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

public class FlowViewer extends AbstractComboViewer<FlowDescriptor> {

	private static final String[] COLUMN_HEADERS = new String[] { M.Name, M.Category, M.Location, " " };
	private static final int[] COLUMN_BOUNDS_PERCENTAGES = new int[] { 30, 60, 10, 0 };

	private EntityCache cache;

	public FlowViewer(Composite parent) {
		super(parent);
		this.cache = Cache.getEntityCache();
		setInput(new FlowDescriptor[0]);
	}

	@Override
	protected int getDisplayColumn() {
		return 3;
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	@Override
	protected int[] getColumnBoundsPercentages() {
		return COLUMN_BOUNDS_PERCENTAGES;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new FlowLabelProvider();
	}

	@Override
	protected ViewerComparator getComparator() {
		return new FlowComparator();
	}

	@Override
	public Class<FlowDescriptor> getType() {
		return FlowDescriptor.class;
	}

	private Category getCategory(FlowDescriptor flow) {
		if (flow == null || flow.category == null)
			return null;
		return cache.get(Category.class, flow.category);
	}

	private Location getLocation(FlowDescriptor flow) {
		if (flow == null || flow.location == null)
			return null;
		return cache.get(Location.class, flow.location);
	}

	private class FlowComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof FlowDescriptor) || e1 == null) {
				if (e2 != null)
					return -1;
				return 0;
			}
			if (!(e2 instanceof FlowDescriptor) || e2 == null)
				return 1;
			FlowDescriptor flow1 = (FlowDescriptor) e1;
			FlowDescriptor flow2 = (FlowDescriptor) e2;

			int flowNameCompare = Strings.compare(flow1.name, flow2.name);
			if (flowNameCompare != 0)
				return flowNameCompare;
			int categoryCompare = compareByCategory(flow1, flow2);
			if (categoryCompare != 0)
				return categoryCompare;
			return compareByLocation(flow1, flow2);
		}

		private int compareByCategory(FlowDescriptor flow1, FlowDescriptor flow2) {
			Category category1 = getCategory(flow1);
			Category category2 = getCategory(flow2);
			if (category1 == null && category2 == null)
				return 0;
			if (category1 == null)
				return -1;
			if (category2 == null)
				return 1;
			String path1 = CategoryPath.getFull(category1);
			String path2 = CategoryPath.getFull(category2);
			return Strings.compare(path1, path2);
		}

		private int compareByLocation(FlowDescriptor flow1, FlowDescriptor flow2) {
			Location location1 = getLocation(flow1);
			Location location2 = getLocation(flow2);
			if (location1 == null && location2 == null)
				return 0;
			if (location1 == null)
				return -1;
			if (location2 == null)
				return 1;
			String code1 = location1 != null ? location1.code : "";
			String code2 = location2 != null ? location2.code : "";
			return Strings.compare(code1, code2);
		}

	}

	private class FlowLabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col == 1)
				return null;
			if (col == 2)
				return Images.get(ModelType.LOCATION);
			FlowDescriptor flow = (FlowDescriptor) element;
			return Images.get(flow);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			FlowDescriptor flow = (FlowDescriptor) element;
			switch (columnIndex) {
			case 0:
				return flow.name;
			case 1:
				if (flow.category == null)
					return null;
				Category category = cache.get(Category.class, flow.category);
				return CategoryPath.getFull(category);
			case 2:
				if (flow.location == null)
					return null;
				return cache.get(Location.class, flow.location).code;
			case 3:
				return fullName(flow);
			default:
				return null;
			}
		}

		private String fullName(FlowDescriptor flow) {
			if (flow == null)
				return null;
			String t = flow.name;
			Category category = getCategory(flow);
			if (category != null)
				t += " - " + CategoryPath.getShort(category);
			Location location = getLocation(flow);
			if (location != null && location.code != null)
				t += " - " + location.code;
			return t;
		}
	}
}
