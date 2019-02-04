package org.openlca.app.rcp.images;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.navigation.Group;
import org.openlca.app.navigation.GroupType;
import org.openlca.app.util.FileType;
import org.openlca.cloud.model.Comments;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Source;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.IResult;

public class Images {

	public static Image get(RootEntity entity) {
		if (entity instanceof Source) {
			Source source = (Source) entity;
			if (source.externalFile != null)
				return get(FileType.forName(source.externalFile));
		}
		if (entity instanceof Process)
			return get(((Process) entity).processType);
		if (entity instanceof Flow)
			return get(((Flow) entity).flowType);
		return get(ModelType.forModelClass(entity.getClass()));
	}

	public static Image get(BaseDescriptor d) {
		if (d == null || d.type == null)
			return null;
		switch (d.type) {
		case PROCESS:
			return get(((ProcessDescriptor) d).processType);
		case FLOW:
			return get(((FlowDescriptor) d).flowType);
		case CATEGORY:
			CategoryDescriptor cd = (CategoryDescriptor) d;
			ModelIcon icon = categoryIcon(cd.categoryType);
			return ImageManager.get(icon);
		default:
			return get(d.type);
		}
	}

	public static Image get(Category c) {
		if (c == null)
			return null;
		ModelIcon icon = categoryIcon(c.modelType);
		if (icon == null)
			return Icon.FOLDER.get();
		return ImageManager.get(icon);
	}

	public static Image get(Group group) {
		if (group == null)
			return null;
		ModelIcon icon = icon(group.type);
		if (icon == null)
			return Icon.FOLDER.get();
		return ImageManager.get(icon);
	}

	public static Image get(ModelType type) {
		ModelIcon icon = icon(type);
		if (icon == null)
			return null;
		return ImageManager.get(icon);
	}

	public static Image get(FlowType type) {
		ModelIcon icon = icon(type);
		if (icon == null)
			return null;
		return ImageManager.get(icon);
	}

	public static Image get(ProcessType type) {
		ModelIcon icon = icon(type);
		if (icon == null)
			return null;
		return ImageManager.get(icon);
	}

	public static Image get(GroupType type) {
		ModelIcon icon = icon(type);
		if (icon == null)
			return Icon.FOLDER.get();
		return ImageManager.get(icon);
	}

	public static Image get(FileType type) {
		if (type == null)
			return null;
		FileIcon icon = icon(type);
		if (icon == null)
			return ImageManager.get(FileIcon.DEFAULT);
		return ImageManager.get(icon);
	}

	public static Image get(ModelType type, Overlay overlay) {
		ModelIcon icon = Images.icon(type);
		if (icon == null)
			return null;
		if (overlay == null)
			return ImageManager.get(icon);
		return ImageManager.get(icon, overlay);
	}

	public static Image get(Boolean value) {
		if (value == null || !value)
			return Icon.CHECK_FALSE.get();
		return Icon.CHECK_TRUE.get();
	}

	public static Image get(Comments comments, String path) {
		if (comments != null && comments.hasPath(path))
			return Icon.SHOW_COMMENTS.get();
		return null;
	}

	public static Image get(IResult result) {
		if (result == null)
			return null;
		if (result instanceof FullResult)
			return Icon.ANALYSIS_RESULT.get();
		return Icon.QUICK_RESULT.get();
	}

	public static Image getForCategory(ModelType type) {
		ModelIcon icon = categoryIcon(type);
		if (icon == null)
			return Icon.FOLDER.get();
		return ImageManager.get(icon);
	}

	public static Image getForCategory(ModelType type, Overlay overlay) {
		ModelIcon icon = Images.categoryIcon(type);
		if (icon == null)
			return null;
		if (overlay == null)
			return ImageManager.get(icon);
		return ImageManager.get(icon, overlay);
	}

	public static ImageDescriptor descriptor(RootEntity entity) {
		if (entity instanceof Process)
			return descriptor(((Process) entity).processType);
		if (entity instanceof Flow)
			return descriptor(((Flow) entity).flowType);
		return descriptor(ModelType.forModelClass(entity.getClass()));
	}

	public static ImageDescriptor descriptor(BaseDescriptor d) {
		if (d == null || d.type == null)
			return null;
		switch (d.type) {
		case PROCESS:
			return descriptor(((ProcessDescriptor) d).processType);
		case FLOW:
			return descriptor(((FlowDescriptor) d).flowType);
		default:
			return descriptor(d.type);
		}
	}

	public static ImageDescriptor descriptor(Category category) {
		if (category == null)
			return null;
		ModelIcon icon = categoryIcon(category.modelType);
		if (icon == null)
			return Icon.FOLDER.descriptor();
		return ImageManager.descriptor(icon);
	}

	public static ImageDescriptor descriptor(Group group) {
		if (group == null)
			return null;
		ModelIcon icon = icon(group.type);
		if (icon == null)
			return Icon.FOLDER.descriptor();
		return ImageManager.descriptor(icon);
	}

	public static ImageDescriptor descriptor(ModelType type) {
		ModelIcon icon = icon(type);
		if (icon == null)
			return null;
		return ImageManager.descriptor(icon);
	}

	public static ImageDescriptor descriptor(FlowType type) {
		ModelIcon icon = icon(type);
		if (icon == null)
			return null;
		return ImageManager.descriptor(icon);
	}

	public static ImageDescriptor descriptor(ProcessType type) {
		ModelIcon icon = icon(type);
		if (icon == null)
			return null;
		return ImageManager.descriptor(icon);
	}

	public static ImageDescriptor descriptor(GroupType type) {
		ModelIcon icon = icon(type);
		if (icon == null)
			return Icon.FOLDER.descriptor();
		return ImageManager.descriptor(icon);
	}

	public static ImageDescriptor descriptor(FileType type) {
		FileIcon icon = icon(type);
		if (icon == null)
			return ImageManager.descriptor(FileIcon.DEFAULT);
		return ImageManager.descriptor(icon);
	}

	public static ImageDescriptor descriptor(ModelType type, Overlay overlay) {
		ModelIcon icon = Images.icon(type);
		if (icon == null)
			return null;
		if (overlay == null)
			return ImageManager.descriptor(icon);
		return ImageManager.descriptor(icon, overlay);
	}

	public static ImageDescriptor descriptor(Boolean value) {
		if (value == null || !value)
			return Icon.CHECK_FALSE.descriptor();
		return Icon.CHECK_TRUE.descriptor();
	}

	public static ImageDescriptor descriptor(Comments comments, String path) {
		if (comments.hasPath(path))
			return Icon.SHOW_COMMENTS.descriptor();
		return null;
	}

	public static ImageDescriptor descriptorForCategory(ModelType type) {
		ModelIcon icon = Images.categoryIcon(type);
		if (icon == null)
			return ImageManager.descriptor(Icon.FOLDER);
		return ImageManager.descriptor(icon);
	}

	public static ImageDescriptor descriptorForCategory(ModelType type, Overlay overlay) {
		ModelIcon icon = Images.categoryIcon(type);
		if (icon == null)
			return ImageManager.descriptor(Icon.FOLDER);
		if (overlay == null)
			return ImageManager.descriptor(icon);
		return ImageManager.descriptor(icon, overlay);
	}

	public static ImageDescriptor wizard(ModelType type) {
		ModelIcon icon = wizardIcon(type);
		if (icon == null)
			return Icon.NEW_WIZARD.descriptor();
		return ImageManager.descriptor(icon);
	}

	public static ImageDescriptor newDatabase() {
		return ImageManager.descriptor(Icon.DATABASE, Overlay.NEW);
	}

	private static ModelIcon wizardIcon(ModelType type) {
		if (type == null)
			return null;
		switch (type) {
		case ACTOR:
			return ModelIcon.ACTOR_WIZARD;
		case CURRENCY:
			return ModelIcon.CURRENCY_WIZARD;
		case FLOW:
			return ModelIcon.FLOW_WIZARD;
		case FLOW_PROPERTY:
			return ModelIcon.FLOW_PROPERTY_WIZARD;
		case IMPACT_METHOD:
			return ModelIcon.IMPACT_METHOD_WIZARD;
		case LOCATION:
			return ModelIcon.LOCATION_WIZARD;
		case PARAMETER:
			return ModelIcon.PARAMETER_WIZARD;
		case PROCESS:
			return ModelIcon.PROCESS_WIZARD;
		case PRODUCT_SYSTEM:
			return ModelIcon.PRODUCT_SYSTEM_WIZARD;
		case PROJECT:
			return ModelIcon.PROJECT_WIZARD;
		case SOCIAL_INDICATOR:
			return ModelIcon.SOCIAL_INDICATOR_WIZARD;
		case SOURCE:
			return ModelIcon.SOURCE_WIZARD;
		case UNIT_GROUP:
			return ModelIcon.UNIT_GROUP_WIZARD;
		case DQ_SYSTEM:
			return ModelIcon.DQ_SYSTEM_WIZARD;
		default:
			return null;
		}
	}

	private static ModelIcon icon(GroupType type) {
		if (type == null)
			return null;
		switch (type) {
		case BACKGROUND_DATA:
			return ModelIcon.GROUP_BACKGROUND_DATA;
		case INDICATORS:
			return ModelIcon.GROUP_INDICATORS;
		case MODELS:
			return ModelIcon.GROUP_MODELS;
		default:
			return null;
		}
	}

	static ModelIcon icon(ModelType type) {
		if (type == null)
			return null;
		switch (type) {
		case ACTOR:
			return ModelIcon.ACTOR;
		case FLOW:
			return ModelIcon.FLOW;
		case FLOW_PROPERTY:
			return ModelIcon.FLOW_PROPERTY;
		case IMPACT_METHOD:
			return ModelIcon.IMPACT_METHOD;
		case IMPACT_CATEGORY:
			return ModelIcon.IMPACT_CATEGORY;
		case PROCESS:
			return ModelIcon.PROCESS;
		case PRODUCT_SYSTEM:
			return ModelIcon.PRODUCT_SYSTEM;
		case PROJECT:
			return ModelIcon.PROJECT;
		case SOURCE:
			return ModelIcon.SOURCE;
		case SOCIAL_INDICATOR:
			return ModelIcon.SOCIAL_INDICATOR;
		case LOCATION:
			return ModelIcon.LOCATION;
		case PARAMETER:
			return ModelIcon.PARAMETER;
		case CURRENCY:
			return ModelIcon.CURRENCY;
		case UNIT_GROUP:
		case UNIT:
			return ModelIcon.UNIT_GROUP;
		case DQ_SYSTEM:
			return ModelIcon.DQ_SYSTEM;
		default:
			return null;
		}
	}

	private static ModelIcon icon(FlowType type) {
		if (type == null)
			return null;
		switch (type) {
		case ELEMENTARY_FLOW:
			return ModelIcon.FLOW_ELEMENTARY;
		case PRODUCT_FLOW:
			return ModelIcon.FLOW_PRODUCT;
		case WASTE_FLOW:
			return ModelIcon.FLOW_WASTE;
		default:
			return null;
		}
	}

	private static ModelIcon icon(ProcessType type) {
		if (type == null)
			return null;
		switch (type) {
		case UNIT_PROCESS:
			return ModelIcon.PROCESS;
		case LCI_RESULT:
			return ModelIcon.PROCESS_SYSTEM;
		default:
			return null;
		}
	}

	private static FileIcon icon(FileType type) {
		if (type == null)
			return null;
		switch (type) {
		case CSV:
			return FileIcon.CSV;
		case EXCEL:
			return FileIcon.EXCEL;
		case IMAGE:
			return FileIcon.IMAGE;
		case MARKUP:
			return FileIcon.MARKUP;
		case PDF:
			return FileIcon.PDF;
		case POWERPOINT:
			return FileIcon.POWERPOINT;
		case WORD:
			return FileIcon.WORD;
		case XML:
			return FileIcon.XML;
		case ZIP:
			return FileIcon.ZIP;
		default:
			return FileIcon.DEFAULT;
		}
	}

	static ModelIcon categoryIcon(ModelType modelType) {
		if (modelType == null)
			return null;
		switch (modelType) {
		case ACTOR:
			return ModelIcon.ACTOR_CATEGORY;
		case FLOW:
			return ModelIcon.FLOW_CATEGORY;
		case FLOW_PROPERTY:
			return ModelIcon.FLOW_PROPERTY_CATEGORY;
		case IMPACT_METHOD:
			return ModelIcon.IMPACT_METHOD_CATEGORY;
		case PROCESS:
			return ModelIcon.PROCESS_CATEGORY;
		case PRODUCT_SYSTEM:
			return ModelIcon.PRODUCT_SYSTEM_CATEGORY;
		case PROJECT:
			return ModelIcon.PROJECT_CATEGORY;
		case SOURCE:
			return ModelIcon.SOURCE_CATEGORY;
		case SOCIAL_INDICATOR:
			return ModelIcon.SOCIAL_INDICATOR_CATEGORY;
		case UNIT_GROUP:
			return ModelIcon.UNIT_GROUP_CATEGORY;
		case LOCATION:
			return ModelIcon.LOCATION_CATEGORY;
		case PARAMETER:
			return ModelIcon.PARAMETER_CATEGORY;
		case CURRENCY:
			return ModelIcon.CURRENCY_CATEGORY;
		case DQ_SYSTEM:
			return ModelIcon.DQ_SYSTEM_CATEGORY;
		default:
			return null;
		}
	}

	/**
	 * Returns the shared image descriptor with the given name from the Eclipse
	 * platform. See ISharedImages for the image names.
	 */
	public static ImageDescriptor platformDescriptor(String name) {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(name);
	}

	/**
	 * Returns the shared image with the given name from the Eclipse platform.
	 * See ISharedImages for the image names.
	 */
	public static Image platformImage(String name) {
		return PlatformUI.getWorkbench().getSharedImages().getImage(name);
	}

}
