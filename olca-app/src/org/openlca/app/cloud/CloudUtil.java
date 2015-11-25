package org.openlca.app.cloud;

import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptors;

public class CloudUtil {

	public static Dataset toDataset(INavigationElement<?> element) {
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
		return toDataset(descriptor, Descriptors.toDescriptor(category));
	}

	public static Dataset toDataset(CategorizedDescriptor entity,
			CategoryDescriptor category) {
		Dataset dataset = new Dataset();
		dataset.setRefId(entity.getRefId());
		dataset.setType(entity.getModelType());
		dataset.setVersion(Version.asString(entity.getVersion()));
		dataset.setLastChange(entity.getLastChange());
		dataset.setName(entity.getName());
		ModelType categoryType = null;
		if (category != null) {
			dataset.setCategoryRefId(category.getRefId());
			categoryType = category.getCategoryType();
		} else {
			if (entity.getModelType() == ModelType.CATEGORY)
				categoryType = ((CategoryDescriptor) entity).getCategoryType();
			else
				categoryType = entity.getModelType();
		}
		dataset.setCategoryType(categoryType);
		return dataset;
	}

	public static Dataset toDataset(CategorizedEntity entity) {
		CategorizedDescriptor descriptor = Descriptors.toDescriptor(entity);
		Category category = entity.getCategory();
		CategoryDescriptor cDescriptor = Descriptors.toDescriptor(category);
		return toDataset(descriptor, cDescriptor);
	}

	public static JsonLoader getJsonLoader(RepositoryClient client) {
		return new JsonLoader(client);
	}

}
