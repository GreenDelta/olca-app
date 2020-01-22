package org.openlca.app.components;

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
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.Category;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

public class ResultFlowCombo extends AbstractComboViewer<IndexFlow> {

	public ResultFlowCombo(Composite parent) {
		super(parent);
		setInput(new IndexFlow[0]);
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
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	@Override
	public Class<IndexFlow> getType() {
		return IndexFlow.class;
	}

	@Override
	protected ViewerComparator getComparator() {
		return new ViewerComparator() {

			private LabelProvider label = new LabelProvider();

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
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

	private class LabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof IndexFlow))
				return null;
			IndexFlow f = (IndexFlow) obj;
			if (col == 0)
				return Images.get(f.flow);
			if (col == 2)
				return Images.get(ModelType.LOCATION);
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof IndexFlow))
				return null;
			IndexFlow f = (IndexFlow) obj;
			if (f.flow == null)
				return null;

			EntityCache cache = Cache.getEntityCache();
			switch (col) {

			case 0:
				// name
				return Labels.getDisplayName(f.flow);

			case 1:
				// category
				if (f.flow.category == null)
					return null;
				Category c = cache.get(Category.class, f.flow.category);
				return CategoryPath.getFull(c);

			case 2:
				// location
				if (f.location == null)
					return null;
				// TODO: probably it would be good to add the
				// location code to the location descriptor
				Location loc = cache.get(Location.class, f.location.id);
				return loc != null ? loc.code : null;

			case 3:
				// full display label
				String s = f.flow.name;
				if (f.flow.category != null) {
					c = cache.get(Category.class, f.flow.category);
					if (c != null) {
						s += " - " + CategoryPath.getShort(c);
					}
				}
				if (f.location != null) {
					loc = cache.get(Location.class, f.location.id);
					if (loc != null) {
						s += " - " + loc.code;
					}
				}
				return s;
			default:
				break;
			}
			return null;
		}
	}

}
