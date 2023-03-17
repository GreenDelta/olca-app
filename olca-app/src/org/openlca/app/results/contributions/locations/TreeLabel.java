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
import org.openlca.core.matrix.index.TechFlow;
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
		if (selection instanceof EnviFlow f) {
			unit = Labels.refUnit(f);
		} else if (selection instanceof FlowDescriptor f) {
			unit = Labels.refUnit(f);
		} else if (selection instanceof ImpactDescriptor i) {
			unit = i.referenceUnit;
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
		if (!(obj instanceof Contribution<?> c))
			return null;
		if (col == 1)
			return image.get(c.share);
		if (col != 0)
			return null;
		if (c.item instanceof Descriptor d)
			return Images.get(d);
		if (c.item instanceof RootEntity e)
			return Images.get(e);
		if (c.item instanceof EnviFlow ef)
			return Images.get(ef);
		if (c.item instanceof TechFlow tf)
			return Images.get(tf);
		return null;
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof Contribution<?> c))
			return null;
		return switch (col) {
			case 0 -> getLabel(c);
			case 1 -> Numbers.format(c.amount);
			case 2 -> unit;
			default -> null;
		};
	}

	private String getLabel(Contribution<?> c) {
		if (c == null || c.item == null)
			return M.None;
		if (c.item instanceof Location loc) {
			String label = loc.name;
			if (loc.code != null
					&& !Strings.nullOrEqual(loc.code, label)) {
				label += " - " + loc.code;
			}
			return label;
		}
		if (c.item instanceof EnviFlow ef)
			return Labels.name(ef);
		if (c.item instanceof TechFlow tf)
			return Labels.name(tf);
		if (c.item instanceof Descriptor d)
			return Labels.name(d);
		if (c.item instanceof RefEntity e)
			return Labels.name(e);
		return null;
	}
}
