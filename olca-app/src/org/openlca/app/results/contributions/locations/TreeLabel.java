package org.openlca.app.results.contributions.locations;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.Location;
import org.openlca.core.model.RefEntity;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.util.Strings;

class TreeLabel extends ColumnLabelProvider implements ITableLabelProvider {

	private String unit = "";
	private final ContributionImage image = new ContributionImage();

	void update(Object selection) {
		if (selection instanceof FlowDescriptor) {
			unit = Labels.refUnit((FlowDescriptor) selection);
		} else if (selection instanceof ImpactDescriptor) {
			unit = ((ImpactDescriptor) selection).referenceUnit;
		} else if (selection instanceof CostResultDescriptor) {
			unit = Labels.getReferenceCurrencyCode();
		} else {
			unit = "";
		}
	}

	@Override
	public void dispose() {
		image.dispose();
		super.dispose();
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (!(obj instanceof Contribution))
			return null;
		Contribution<?> c = (Contribution<?>) obj;
		if (col == 1)
			return image.get(c.share);
		if (col != 0)
			return null;
		if (c.item instanceof Descriptor)
			return Images.get((Descriptor) c.item);
		if (c.item instanceof RootEntity)
			return Images.get((RootEntity) c.item);
		if (c.item instanceof EnviFlow)
			return Images.get(((EnviFlow) c.item).flow());
		return null;
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof Contribution))
			return null;
		Contribution<?> c = (Contribution<?>) obj;
		switch (col) {
		case 0:
			return getLabel(c);
		case 1:
			return Numbers.format(c.amount);
		case 2:
			return unit;
		default:
			return null;
		}
	}

	private String getLabel(Contribution<?> c) {
		if (c == null || c.item == null)
			return M.None;
		if (c.item instanceof Location) {
			Location loc = (Location) c.item;
			String label = loc.name;
			if (loc.code != null
					&& !Strings.nullOrEqual(loc.code, label)) {
				label += " - " + loc.code;
			}
			return label;
		}
		if (c.item instanceof EnviFlow)
			return Labels.name((EnviFlow) c.item);
		if (c.item instanceof Descriptor)
			return Labels.name((Descriptor) c.item);
		if (c.item instanceof RefEntity)
			return Labels.name((RefEntity) c.item);
		return null;
	}
}
