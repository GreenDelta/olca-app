package org.openlca.app.cloud;

import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.DatasetDescriptor;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptors;

public class CloudUtil {

	public static DatasetDescriptor toDescriptor(INavigationElement<?> element) {
		CategorizedDescriptor descriptor = null;
		if (element instanceof CategoryElement) {
			descriptor = Descriptors.toDescriptor(((CategoryElement) element)
					.getContent());
		} else if (element instanceof ModelElement)
			descriptor = ((ModelElement) element).getContent();
		if (descriptor == null)
			return null;
		Category category = null;
		if (element.getParent() instanceof CategoryElement)
			category = ((CategoryElement) element.getParent()).getContent();
		return toDescriptor(descriptor, Descriptors.toDescriptor(category));
	}

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

	public static JsonLoader getJsonLoader(RepositoryClient client) {
		return new JsonLoader(client);
	}
	
}
