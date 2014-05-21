package org.openlca.app.util;

import org.openlca.app.editors.graphical.layout.constraints.NodeLayoutStore;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

public class Resources {

	public static void cleanup(BaseDescriptor descriptor) {
		switch (descriptor.getModelType()) {
		case PRODUCT_SYSTEM:
			cleanup((ProductSystemDescriptor) descriptor);
			break;
		default:
			break;
		}
	}

	private static void cleanup(ProductSystemDescriptor descriptor) {
		NodeLayoutStore.deleteLayout(descriptor.getId());
	}

}
