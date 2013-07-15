package org.openlca.core.editors.lciamethod;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NormalizationWeightingFactor;

/**
 * Label provider for normalization and weighting factors.
 */
class NwSetFactorLabel extends BaseLabelProvider implements ITableLabelProvider {

	private ImpactMethod method;

	public NwSetFactorLabel(ImpactMethod method) {
		this.method = method;
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof NormalizationWeightingFactor))
			return null;
		NormalizationWeightingFactor factor = (NormalizationWeightingFactor) element;
		switch (columnIndex) {
		case 0:
			return categoryName(factor);
		case 1:
			return value(factor.getNormalizationFactor());
		case 2:
			return value(factor.getWeightingFactor());
		default:
			return null;
		}
	}

	private String value(Double value) {
		if (value == null)
			return "-";
		return value.toString();
	}

	private String categoryName(NormalizationWeightingFactor factor) {
		long catId = factor.getImpactCategoryId();
		if (method == null)
			return null;
		for (ImpactCategory cat : method.getLCIACategories())
			if (catId == cat.getId())
				return cat.getName();
		return null;
	}

}