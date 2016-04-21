package org.openlca.app.results.contributions.locations;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.util.Strings;

class TreeLabel extends ColumnLabelProvider implements ITableLabelProvider {

	String unit = "";

	private ContributionImage image = new ContributionImage(
			Display.getCurrent());

	@Override
	public void dispose() {
		image.dispose();
		super.dispose();
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (col != 0)
			return null;
		ContributionItem<?> item = null;
		if (obj instanceof LocationItem) {
			LocationItem element = (LocationItem) obj;
			item = element.contribution;
		} else {
			item = ContributionItem.class.cast(obj);
		}
		return image.getForTable(item.share);
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (obj instanceof LocationItem) {
			LocationItem e = (LocationItem) obj;
			return getText(e.contribution, col);
		}
		if (obj instanceof ContributionItem) {
			ContributionItem<?> item = (ContributionItem<?>) obj;
			return getText(item, col);
		}
		return null;
	}

	private String getText(ContributionItem<?> ci, int col) {
		switch (col) {
		case 0:
			return getLabel(ci);
		case 1:
			return Numbers.format(ci.amount);
		case 2:
			return unit;
		default:
			return null;
		}
	}

	private String getLabel(ContributionItem<?> ci) {
		if (ci == null || ci.item == null)
			return null;
		if (ci.item instanceof BaseDescriptor)
			return Labels.getDisplayName((BaseDescriptor) ci.item);
		if (ci.item instanceof Location) {
			Location loc = (Location) ci.item;
			String label = loc.getName();
			if (loc.getCode() != null && !Strings.nullOrEqual(loc.getCode(), label)) {
				label += " - " + loc.getCode();
			}
			return label;
		}
		return null;
	}

}