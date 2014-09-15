package org.openlca.app.util;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Source;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class Images {

	public static Image getIcon(RootEntity entity) {
		if (entity instanceof Source) {
			Source source = (Source) entity;
			if (source.getExternalFile() != null)
				return ImageType.forFile(source.getExternalFile()).get();
		}
		return getIcon(getType(entity));
	}

	public static Image getIcon(BaseDescriptor descriptor) {
		if (descriptor == null)
			return null;
		else
			return getIcon(descriptor.getModelType());
	}

	public static Image getIcon(ModelType type) {
		ImageType imageType = getImageType(type);
		if (imageType == null)
			return null;
		return imageType.get();
	}

	public static Image getIcon(FlowType type) {
		ImageType imageType = getImageType(type);
		if (imageType == null)
			return null;
		return imageType.get();
	}

	public static Image getIcon(Category category) {
		ImageType imageType = getImageType(category);
		if (imageType == null)
			return null;
		return imageType.get();
	}

	public static ImageDescriptor getIconDescriptor(RootEntity entity) {
		return getIconDescriptor(getType(entity));
	}

	public static ImageDescriptor getIconDescriptor(ModelType type) {
		ImageType imageType = getImageType(type);
		if (imageType == null)
			return null;
		return imageType.getDescriptor();
	}

	public static ImageDescriptor getIconDescriptor(FlowType type) {
		ImageType imageType = getImageType(type);
		if (imageType == null)
			return null;
		return imageType.getDescriptor();
	}

	public static ImageDescriptor getIconDescriptor(Category category) {
		ImageType imageType = getImageType(category);
		if (imageType == null)
			return null;
		return imageType.getDescriptor();
	}

	private static ImageType getImageType(ModelType type) {
		if (type == null)
			return null;
		switch (type) {
		case ACTOR:
			return ImageType.ACTOR_ICON;
		case FLOW:
			return ImageType.FLOW_ICON;
		case FLOW_PROPERTY:
			return ImageType.FLOW_PROPERTY_ICON;
		case IMPACT_METHOD:
			return ImageType.LCIA_ICON;
		case IMPACT_CATEGORY:
			return ImageType.LCIA_CATEGORY_ICON;
		case PROCESS:
			return ImageType.PROCESS_ICON;
		case PRODUCT_SYSTEM:
			return ImageType.PRODUCT_SYSTEM_ICON;
		case PROJECT:
			return ImageType.PROJECT_ICON;
		case SOURCE:
			return ImageType.SOURCE_ICON;
		case UNIT_GROUP:
		case UNIT:
			return ImageType.UNIT_GROUP_ICON;
		default:
			return null;
		}
	}

	private static ImageType getImageType(FlowType type) {
		if (type == null)
			return null;
		switch (type) {
		case ELEMENTARY_FLOW:
			return ImageType.FLOW_SUBSTANCE;
		case PRODUCT_FLOW:
			return ImageType.FLOW_PRODUCT;
		case WASTE_FLOW:
			return ImageType.FLOW_WASTE;
		default:
			return null;
		}
	}

	private static ImageType getImageType(Category category) {
		if (category == null)
			return null;
		ModelType modelType = category.getModelType();
		if (modelType == null)
			return null;
		switch (modelType) {
		case ACTOR:
			return ImageType.ACTOR_CATEGORY_ICON;
		case FLOW:
			return ImageType.FLOW_CATEGORY_ICON;
		case FLOW_PROPERTY:
			return ImageType.FLOW_PROPERTY_CATEGORY_ICON;
		case IMPACT_METHOD:
			return ImageType.LCIA_CATEGORY_ICON;
		case PROCESS:
			return ImageType.PROCESS_CATEGORY_ICON;
		case PRODUCT_SYSTEM:
			return ImageType.PRODUCT_SYSTEM_CATEGORY_ICON;
		case PROJECT:
			return ImageType.PROJECT_CATEGORY_ICON;
		case SOURCE:
			return ImageType.SOURCE_CATEGORY_ICON;
		case UNIT_GROUP:
			return ImageType.UNIT_GROUP_CATEGORY_ICON;
		default:
			return ImageType.COLLAPSE_ICON;
		}
	}

	private static ModelType getType(RootEntity entity) {
		if (entity == null)
			return null;
		Class<?> clazz = entity.getClass();
		if (clazz.equals(Actor.class))
			return ModelType.ACTOR;
		if (clazz.equals(Source.class))
			return ModelType.SOURCE;
		if (clazz.equals(UnitGroup.class))
			return ModelType.UNIT_GROUP;
		if (clazz.equals(Flow.class))
			return ModelType.FLOW;
		if (clazz.equals(FlowProperty.class))
			return ModelType.FLOW_PROPERTY;
		if (clazz.equals(ImpactMethod.class))
			return ModelType.IMPACT_METHOD;
		if (clazz.equals(Project.class))
			return ModelType.PROJECT;
		if (clazz.equals(Process.class))
			return ModelType.PROCESS;
		if (clazz.equals(ProductSystem.class))
			return ModelType.PRODUCT_SYSTEM;
		if (clazz.equals(Unit.class))
			return ModelType.UNIT;
		if (clazz.equals(ImpactCategory.class))
			return ModelType.IMPACT_CATEGORY;
		return null;
	}

}
