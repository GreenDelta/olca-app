package org.openlca.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.core.model.ModelType;
import org.openlca.core.resources.ImageType;

public class Images {

	/**
	 * Returns the image descriptor for the icon (16 x 16 px) for the given
	 * model type.
	 */
	public static ImageDescriptor getIconDescriptor(ModelType modelType) {
		if (modelType == null)
			return null;
		switch (modelType) {
		case ACTOR:
			return ImageType.ACTOR_ICON.getDescriptor();
		case FLOW:
			return ImageType.FLOW_ICON.getDescriptor();
		case FLOW_PROPERTY:
			return ImageType.FLOW_PROPERTY_ICON.getDescriptor();
		case IMPACT_METHOD:
			return ImageType.LCIA_CATEGORY_ICON.getDescriptor();
		case PROCESS:
			return ImageType.PROCESS_ICON.getDescriptor();
		case PRODUCT_SYSTEM:
			return ImageType.PRODUCT_SYSTEM_ICON.getDescriptor();
		case PROJECT:
			return ImageType.PROJECT_ICON.getDescriptor();
		case SOURCE:
			return ImageType.SOURCE_ICON.getDescriptor();
		case UNIT_GROUP:
			return ImageType.UNIT_GROUP_ICON.getDescriptor();
		default:
			return null;
		}
	}

}
