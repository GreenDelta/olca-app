package org.openlca.app.cloud;

import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptors;

import com.greendelta.cloud.model.data.DatasetDescriptor;

public class CloudUtil {

	public static DatasetDescriptor toDescriptor(CategorizedDescriptor entity,
			CategoryDescriptor category) {
		DatasetDescriptor descriptor = new DatasetDescriptor();
		descriptor.setRefId(entity.getRefId());
		descriptor.setType(entity.getModelType());
		descriptor.setVersion(Version.asString(entity.getVersion()));
		descriptor.setLastChange(entity.getLastChange());
		descriptor.setName(entity.getName());
		ModelType categoryType = null;
		if (category != null) {
			descriptor.setCategoryRefId(category.getRefId());
			categoryType = category.getCategoryType();
		} else {
			if (entity.getModelType() == ModelType.CATEGORY)
				categoryType = ((CategoryDescriptor) entity).getCategoryType();
			else
				categoryType = entity.getModelType();
		}
		descriptor.setCategoryType(categoryType);
		return descriptor;
	}

	public static DatasetDescriptor toDescriptor(CategorizedEntity entity) {
		return toDescriptor(Descriptors.toDescriptor(entity),
				Descriptors.toDescriptor(entity.getCategory()));
	}
}
