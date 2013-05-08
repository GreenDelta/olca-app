package org.openlca.core.editors.lciamethod;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.core.model.NormalizationWeightingSet;

class NwSetLabelProvider extends BaseLabelProvider implements
		ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof NormalizationWeightingSet))
			return null;
		NormalizationWeightingSet set = (NormalizationWeightingSet) element;
		switch (columnIndex) {
		case 0:
			return set.getReferenceSystem();
		case 1:
			return set.getUnit();
		default:
			return null;
		}
	}

}