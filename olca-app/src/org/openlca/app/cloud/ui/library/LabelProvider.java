package org.openlca.app.cloud.ui.library;

import java.util.Map.Entry;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.util.Images;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.ModelType;

class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex != 0)
			return null;
		Entry<Dataset, String> entry = (Entry<Dataset, String>) element;
		Dataset dataset = entry.getKey();
		if (dataset.getType() == ModelType.CATEGORY)
			return Images.getCategoryIcon(dataset.getCategoryType());
		return Images.getIcon(dataset.getType());
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getColumnText(Object element, int columnIndex) {
		Entry<Dataset, String> entry = (Entry<Dataset, String>) element;
		switch (columnIndex) {
		case 0:
			Dataset dataset = entry.getKey();
			return dataset.getFullPath();
		case 1:
			return entry.getValue();
		default:
			return null;
		}
	}
}
