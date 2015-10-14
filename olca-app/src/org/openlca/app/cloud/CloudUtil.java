package org.openlca.app.cloud;

import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptors;

import com.greendelta.cloud.model.data.DatasetIdentifier;

public class CloudUtil {

	public static DatasetIdentifier toIdentifier(
			CategorizedDescriptor descriptor, CategoryDescriptor category) {
		DatasetIdentifier identifier = new DatasetIdentifier();
		identifier.setRefId(descriptor.getRefId());
		identifier.setType(descriptor.getModelType());
		identifier.setVersion(Version.asString(descriptor.getVersion()));
		identifier.setLastChange(descriptor.getLastChange());
		identifier.setName(descriptor.getName());
		ModelType categoryType = null;
		if (category != null) {
			identifier.setCategoryRefId(category.getRefId());
			categoryType = category.getCategoryType();
		} else {
			if (descriptor.getModelType() == ModelType.CATEGORY)
				categoryType = ((CategoryDescriptor) descriptor)
						.getCategoryType();
			else
				categoryType = descriptor.getModelType();
		}
		identifier.setCategoryType(categoryType);
		return identifier;
	}

	public static DatasetIdentifier toIdentifier(CategorizedEntity entity) {
		return toIdentifier(Descriptors.toDescriptor(entity),
				Descriptors.toDescriptor(entity.getCategory()));
	}
}
