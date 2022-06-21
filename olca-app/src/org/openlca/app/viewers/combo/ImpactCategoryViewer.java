package org.openlca.app.viewers.combo;

import java.util.Collection;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.util.Strings;

public class ImpactCategoryViewer extends AbstractComboViewer<ImpactDescriptor> {

	private static final String[] COLUMN_HEADERS = new String[] {
			M.Name, M.Unit };
	private static final int[] COLUMN_BOUNDS_PERCENTAGES = new int[] { 80, 20 };

	public ImpactCategoryViewer(Composite parent) {
		super(parent);
		setInput(new ImpactDescriptor[0]);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	@Override
	protected ViewerComparator getComparator() {
		return new ImpactCategoryComparator();
	}

	@Override
	protected int[] getColumnBoundsPercentages() {
		return COLUMN_BOUNDS_PERCENTAGES;
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	public void setInput(Collection<ImpactDescriptor> categories) {
		setInput(categories.toArray(new ImpactDescriptor[0]));
	}

	@Override
	public Class<ImpactDescriptor> getType() {
		return ImpactDescriptor.class;
	}

	private static class LabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			return column == 0
					? Images.get(ModelType.IMPACT_CATEGORY)
					: null;
		}

		@Override
		public String getColumnText(Object element, int column) {
			var category = (ImpactDescriptor) element;
			return switch (column) {
				case 0 -> Labels.name(category);
				case 1 -> category.referenceUnit;
				default -> "";
			};
		}

	}

	private static class ImpactCategoryComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof ImpactDescriptor i1)) {
				if (e2 != null)
					return -1;
				return 0;
			}
			if (!(e2 instanceof ImpactDescriptor i2))
				return 1;
			int c = Strings.compare(i1.name, i2.name);
			if (c != 0)
				return c;
			return Strings.compare(i1.referenceUnit, i2.referenceUnit);
		}

	}

}
