package org.openlca.app.viewers.combo;

import org.eclipse.jface.viewers.BaseLabelProvider;
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

	private final EntityCache cache;
	private LabelProvider _label;

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
		return new String[] { M.Name, M.Category, M.Location, " " };
	}

	@Override
	protected int[] getColumnBoundsPercentages() {
		return new int[] { 30, 60, 10, 0 };
	}

	@Override
	protected LabelProvider getLabelProvider() {
		if (_label == null) {
			_label = new LabelProvider();
		}
		return _label;
	}

	@Override
	protected ViewerComparator getComparator() {

		return new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				LabelProvider label = getLabelProvider();
				for (int col = 0; col < 3; col++) {
					String s1 = label.getColumnText(e1, col);
					String s2 = label.getColumnText(e2, col);
					int c = Strings.compare(s1, s2);
					if (c != 0)
						return c;
				}
				return 0;
			}
		};
	}

	@Override
	public Class<FlowDescriptor> getType() {
		return FlowDescriptor.class;
	}

	private class LabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof FlowDescriptor))
				return null;
			var d = (FlowDescriptor) obj;
			if (col == 0)
				return Images.get(d);
			if (col == 2 && d.location != null)
				return Images.get(ModelType.LOCATION);
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof FlowDescriptor))
				return null;
			var flow = (FlowDescriptor) obj;
			switch (col) {
			case 0:
				return flow.name;
			case 1:
				if (flow.category == null)
					return null;
				var category = cache.get(Category.class, flow.category);
				return CategoryPath.getFull(category);
			case 2:
				if (flow.location == null)
					return null;
				Location loc = cache.get(Location.class, flow.location);
				return loc != null ? loc.code : null;
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
	}
}
