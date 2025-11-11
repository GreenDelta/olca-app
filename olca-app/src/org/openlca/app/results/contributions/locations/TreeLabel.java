package org.openlca.app.results.contributions.locations;

import java.util.Objects;

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

class TreeLabel extends ColumnLabelProvider implements ITableLabelProvider {

	private String unit = "";
	private final ContributionImage image = new ContributionImage();

	void update(Object selection) {
		unit = switch (selection) {
			case EnviFlow f -> Labels.refUnit(f);
			case FlowDescriptor f -> Labels.refUnit(f);
			case ImpactDescriptor i -> i.referenceUnit;
			case CostResultDescriptor ignored -> Labels.getReferenceCurrencyCode();
			case null, default -> "";
		};
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
		return switch (c.item) {
			case Descriptor d -> Images.get(d);
			case RootEntity e -> Images.get(e);
			case EnviFlow ef -> Images.get(ef);
			case TechFlow tf -> Images.get(tf);
			case null, default -> null;
		};
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
		return switch (c.item) {
			case Location loc -> {
				var label = loc.name;
				if (loc.code != null && !Objects.equals(loc.code, label)) {
					label += " - " + loc.code;
				}
				yield label;
			}
			case EnviFlow ef -> Labels.name(ef);
			case TechFlow tf -> Labels.name(tf);
			case Descriptor d -> Labels.name(d);
			case RefEntity e -> Labels.name(e);
			default -> null;
		};
	}

}
