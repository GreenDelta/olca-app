package org.openlca.app.db;

import org.apache.commons.io.FileUtils;
import org.openlca.app.editors.graphical.layout.NodeLayoutStore;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

/**
 * Provides helper methods for the management of external resources of entities
 * (like shape-files for LCIA methods or layout files of product systems).
 */
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

	public static void copy(CategorizedEntity original, CategorizedEntity copy) {
		if (original == null || copy == null)
			return;
		if (!Objects.equals(original.getClass(), copy.getClass()))
			return;
		if (original.getRefId() == null || copy.getRefId() == null) {
			Logger log = LoggerFactory.getLogger(Resources.class);
			log.warn("cannot copy resources of {} to {} because one of these " +
					"entities has no reference ID. (Reference IDs are used to "
					+
					"identify external resources)");
			return;
		}
		if (original instanceof ImpactMethod)
			copyMethodResources(original, copy);
		else if (original instanceof ProductSystem)
			copySystemResources(original, copy);
	}

	private static void copyMethodResources(CategorizedEntity original,
			CategorizedEntity copy) {
		File originalDir = DatabaseFolder.getShapeFileLocation(Database.get(),
				original.getRefId());
		if (!originalDir.exists())
			return;
		File copyDir = DatabaseFolder.getShapeFileLocation(Database.get(),
				copy.getRefId());
		try {
			FileUtils.copyDirectory(originalDir, copyDir);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Resources.class);
			log.error("failed to copy shape-files of " + original + " to "
					+ copy, e);
		}
	}

	private static void copySystemResources(CategorizedEntity original,
			CategorizedEntity copy) {
		File originalFile = NodeLayoutStore.getLayoutFile(original.getRefId());
		if (!originalFile.exists())
			return;
		File copyFile = NodeLayoutStore.getLayoutFile(copy.getRefId());
		try {
			FileUtils.copyFile(originalFile, copyFile);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Resources.class);
			log.error("failed to copy layout-files of " + original + " to "
					+ copy, e);
		}
	}
}
