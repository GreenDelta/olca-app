package org.openlca.app.viewers.combo;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.util.Images;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Category;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

public class FlowViewer extends AbstractComboViewer<FlowDescriptor> {

	private static final String[] COLUMN_HEADERS = new String[] {
			Messages.Name, Messages.Category, Messages.Location, " " };
	private static final int[] COLUMN_BOUNDS_PERCENTAGES = new int[] { 30, 30,
			10, 30 };

	private EntityCache cache;

	public FlowViewer(Composite parent, EntityCache cache) {
		super(parent);
		this.cache = cache;
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
	protected ViewerSorter getSorter() {
		return new FlowSorter();
	}

	@Override
	public Class<FlowDescriptor> getType() {
		return FlowDescriptor.class;
	}

	private Category getCategory(FlowDescriptor flow) {
		if (flow == null || flow.getCategory() == null)
			return null;
		return cache.get(Category.class, flow.getCategory());
	}

	private Location getLocation(FlowDescriptor flow) {
		if (flow == null || flow.getLocation() == null)
			return null;
		return cache.get(Location.class, flow.getLocation());
	}

	private class FlowSorter extends ViewerSorter {

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

			int flowNameCompare = Strings.compare(flow1.getName(),
					flow2.getName());
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
			String code1 = location1 != null ? location1.getCode() : "";
			String code2 = location2 != null ? location2.getCode() : "";
			return Strings.compare(code1, code2);
		}

	}

	private class FlowLabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col != 0 && col != 3)
				return null;
			FlowDescriptor flow = (FlowDescriptor) element;
			return Images.getIcon(flow.getFlowType());
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			FlowDescriptor flow = (FlowDescriptor) element;
			switch (columnIndex) {
			case 0:
				return flow.getName();
			case 1:
				if (flow.getCategory() == null)
					return null;
				Category category = cache.get(Category.class,
						flow.getCategory());
				return CategoryPath.getFull(category);
			case 2:
				if (flow.getLocation() == null)
					return null;
				return cache.get(Location.class, flow.getLocation()).getCode();
			case 3:
				return fullName(flow);
			default:
				return null;
			}
		}

		private String fullName(FlowDescriptor flow) {
			if (flow == null)
				return null;
			String t = flow.getName();
			Category category = getCategory(flow);
			if (category != null)
				t += " - " + CategoryPath.getShort(category);
			Location location = getLocation(flow);
			if (location != null && location.getCode() != null)
				t += " - " + location.getCode();
			return t;
		}
	}
}
