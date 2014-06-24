package org.openlca.app.db;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.openlca.app.editors.graphical.layout.NodeLayoutStore;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Resources {

	public static void cleanup(BaseDescriptor descriptor) {
		if (descriptor == null || descriptor.getModelType() == null)
			return;
		switch (descriptor.getModelType()) {
		case PRODUCT_SYSTEM:
			NodeLayoutStore.deleteLayout(descriptor.getRefId());
			break;
		case IMPACT_METHOD:
			deleteShapeFiles(descriptor);
			break;
		default:
			break;
		}
	}

	private static void deleteShapeFiles(BaseDescriptor descriptor) {
		File shapeFileDir = DatabaseFolder.getShapeFileLocation(Database.get(),
				descriptor.getRefId());
		if (shapeFileDir == null || !shapeFileDir.exists())
			return;
		try {
			FileUtils.deleteDirectory(shapeFileDir);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Resources.class);
			log.error("failed to delete shape file folder for " + descriptor, e);
		}
	}

}
