package org.openlca.app.cloud.ui.library;

import java.util.Map.Entry;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.rcp.images.Images;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.ModelType;

class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements ITableLabelProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Image getColumnImage(Object element, int column) {
		if (column != 0)
			return null;
		Entry<Dataset, String> entry = (Entry<Dataset, String>) element;
		Dataset dataset = entry.getKey();
		if (dataset.type == ModelType.CATEGORY)
			return Images.getForCategory(dataset.categoryType);
		return Images.get(dataset.type);
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getColumnText(Object element, int column) {
		Entry<Dataset, String> entry = (Entry<Dataset, String>) element;
		switch (column) {
		case 0:
			Dataset dataset = entry.getKey();
			return dataset.fullPath;
		case 1:
			return entry.getValue();
		default:
			return null;
		}
	}

}
