package org.openlca.app;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.resources.ImageType;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;

public class Images {

	public static Image getIcon(RootEntity entity) {
		return getIcon(getType(entity));
	}

	public static Image getIcon(ModelType modelType) {
		if (modelType == null)
			return null;
		switch (modelType) {
		case ACTOR:
			return ImageType.ACTOR_ICON.get();
		case FLOW:
			return ImageType.FLOW_ICON.get();
		case FLOW_PROPERTY:
			return ImageType.FLOW_PROPERTY_ICON.get();
		case IMPACT_METHOD:
			return ImageType.LCIA_CATEGORY_ICON.get();
		case PROCESS:
			return ImageType.PROCESS_ICON.get();
		case PRODUCT_SYSTEM:
			return ImageType.PRODUCT_SYSTEM_ICON.get();
		case PROJECT:
			return ImageType.PROJECT_ICON.get();
		case SOURCE:
			return ImageType.SOURCE_ICON.get();
		case UNIT_GROUP:
			return ImageType.UNIT_GROUP_ICON.get();
		default:
			return null;
		}
	}

	public static ImageDescriptor getIconDescriptor(RootEntity entity) {
		return getIconDescriptor(getType(entity));
	}

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
		return null;
	}

}
