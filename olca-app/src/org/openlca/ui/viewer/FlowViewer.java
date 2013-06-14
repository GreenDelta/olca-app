package org.openlca.ui.viewer;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.IResultData;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Location;
import org.openlca.core.resources.ImageType;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowViewer extends AbstractComboViewer<Flow> {

	private static final String[] COLUMN_HEADERS = new String[] { "Name",
			"Category", "Location" };
	private static final int[] COLUMN_BOUNDS_PERCENTAGES = new int[] { 45, 40,
			15 };

	private CategoryDao categoryDao;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public FlowViewer(Composite parent) {
		super(parent);
		setInput(new Flow[0]);
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

	public void setDatabase(IDatabase database) {
		categoryDao = new CategoryDao(database.getEntityFactory());
	}

	public void setInput(IResultData result) {
		if (categoryDao == null)
			throw new IllegalStateException("No database set");
		setInput(result.getFlows());
	}

	private Category getCategory(String id) {
		try {
			return categoryDao.getForId(id);
		} catch (Exception e) {
			log.error("Error loading category", e);
		}
		return null;
	}

	private class FlowSorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof Flow) || e1 == null) {
				if (e2 != null)
					return -1;
				return 0;
			}
			if (!(e2 instanceof Flow) || e2 == null)
				return 1;

			Flow flow1 = (Flow) e1;
			Flow flow2 = (Flow) e2;

			// safe compare flow names
			int flowNameCompare = Strings.compare(flow1.getName(),
					flow2.getName());
			if (flowNameCompare != 0)
				return flowNameCompare;

			// compare categories
			Category category1 = getCategory(flow1.getCategoryId());
			Category category2 = getCategory(flow2.getCategoryId());
			int categoryCompare = compare(category1, category2);
			if (categoryCompare != 0)
				return categoryCompare;

			// compare locations
			return compare(flow1.getLocation(), flow2.getLocation());
		}

		private int compare(Category category1, Category category2) {
			String path1 = category1 != null ? category1.getFullPath() : null;
			String path2 = category2 != null ? category2.getFullPath() : null;
			return Strings.compare(path1, path2);
		}

		private int compare(Location location1, Location location2) {
			String locationName1 = location1 != null ? location1.getName()
					: null;
			String locationName2 = location2 != null ? location2.getName()
					: null;
			return Strings.compare(locationName1, locationName2);
		}
	}

	private class FlowLabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			Flow flow = (Flow) element;

			switch (columnIndex) {
			case 0:
				switch (flow.getFlowType()) {
				case ElementaryFlow:
					return ImageType.FLOW_SUBSTANCE.get();
				case ProductFlow:
					return ImageType.FLOW_PRODUCT.get();
				case WasteFlow:
					return ImageType.FLOW_WASTE.get();
				}
			}

			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			Flow flow = (Flow) element;

			switch (columnIndex) {
			case 0:
				return flow.getName();
			case 1:
				Category category = getCategory(flow.getCategoryId());
				if (category == null)
					break;
				return category.getFullPath();
			case 2:
				if (flow.getLocation() == null)
					break;
				return flow.getLocation().getName();
			}
			return "";
		}

	}

}
