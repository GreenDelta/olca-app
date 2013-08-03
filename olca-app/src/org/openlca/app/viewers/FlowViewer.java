package org.openlca.app.viewers;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.CategoryPath;
import org.openlca.core.math.IResultData;
import org.openlca.core.model.Category;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.util.Strings;

public class FlowViewer extends AbstractComboViewer<FlowDescriptor> {

	private static final String[] COLUMN_HEADERS = new String[] { "Name",
			"Category", "Location" };
	private static final int[] COLUMN_BOUNDS_PERCENTAGES = new int[] { 45, 40,
			15 };

	public FlowViewer(Composite parent) {
		super(parent);
		setInput(new FlowDescriptor[0]);
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

	public void setInput(IResultData result) {
		setInput(result.getFlows());
	}

	@Override
	public Class<FlowDescriptor> getType() {
		return FlowDescriptor.class;
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

			// safe compare flow names
			int flowNameCompare = Strings.compare(flow1.getName(),
					flow2.getName());
			if (flowNameCompare != 0)
				return flowNameCompare;

			// compare categories
			Category category1 = flow1.getCategory();
			Category category2 = flow2.getCategory();
			int categoryCompare = compare(category1, category2);
			if (categoryCompare != 0)
				return categoryCompare;

			// compare locations
			return Strings.compare(flow1.getLocationCode(),
					flow2.getLocationCode());
		}

		private int compare(Category category1, Category category2) {
			String path1 = CategoryPath.getFull(category1);
			String path2 = CategoryPath.getFull(category2);
			return Strings.compare(path1, path2);
		}

	}

	private class FlowLabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			FlowDescriptor flow = (FlowDescriptor) element;

			switch (columnIndex) {
			case 0:
				switch (flow.getFlowType()) {
				case ELEMENTARY_FLOW:
					return ImageType.FLOW_SUBSTANCE.get();
				case PRODUCT_FLOW:
					return ImageType.FLOW_PRODUCT.get();
				case WASTE_FLOW:
					return ImageType.FLOW_WASTE.get();
				}
			}

			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			FlowDescriptor flow = (FlowDescriptor) element;

			switch (columnIndex) {
			case 0:
				return flow.getName();
			case 1:
				return CategoryPath.getFull(flow.getCategory());
			case 2:
				if (flow.getLocationCode() == null)
					break;
				return flow.getLocationCode();
			}
			return "";
		}

	}

}
