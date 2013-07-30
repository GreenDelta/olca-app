package org.openlca.core.editors.result;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.Numbers;
import org.openlca.core.model.results.LCIACategoryResult;

/**
 * The label provider for impact assessment results.
 */
class ImpactResultLabel extends LabelProvider implements ITableLabelProvider {

	private int type;
	private String weightingUnit;

	public ImpactResultLabel(int impactType) {
		this.type = impactType;
	}

	public void setWeightingUnit(String unit) {
		this.weightingUnit = unit;
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0)
			return ImageType.LCIA_ICON.get();
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof LCIACategoryResult))
			return null;
		LCIACategoryResult result = (LCIACategoryResult) element;
		switch (columnIndex) {
		case 0:
			return result.getCategory();
		case 1:
			return valueText(result);
		case 2:
			return unitText(result);
		default:
			return null;
		}
	}

	private String valueText(LCIACategoryResult result) {
		switch (type) {
		case CharacterizationPage.IMPACT_ASSESSMENT:
			return Numbers.format(result.getValue());
		case CharacterizationPage.NORMALIZATION:
			return Numbers.format(result.getNormalizedValue());
		case CharacterizationPage.WEIGHTING:
			return Numbers.format(result.getWeightedValue());
		default:
			return null;
		}
	}

	private String unitText(LCIACategoryResult result) {
		switch (type) {
		case CharacterizationPage.IMPACT_ASSESSMENT:
			return result.getUnit();
		case CharacterizationPage.WEIGHTING:
			return weightingUnit;
		default:
			return null;
		}
	}
}