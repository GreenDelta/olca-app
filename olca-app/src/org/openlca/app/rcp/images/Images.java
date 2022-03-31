package org.openlca.app.rcp.images;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.navigation.elements.Group;
import org.openlca.app.navigation.elements.GroupType;
import org.openlca.app.util.FileType;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.RefEntity;
import org.openlca.core.model.Source;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.IResult;

public class Images {

	public static Image get(EnviFlow f) {
		if (f == null)
			return null;
		return f.isVirtual() && f.wrapped() != null
			? get(f.wrapped())
			: get(f.flow());
	}

	public static Image get(RefEntity entity) {
		if (entity instanceof Source source) {
			if (source.externalFile != null)
				return get(FileType.forName(source.externalFile));
		}
		if (entity instanceof Process process)
			return get(process.processType);
		if (entity instanceof Flow flow)
			return get(flow.flowType);
		return get(ModelType.forModelClass(entity.getClass()));
	}

	public static Image get(Descriptor d) {
		if (d == null || d.type == null)
			return null;
		if (d instanceof ProcessDescriptor p && p.processType != null)
			return get(p.processType);
		if (d instanceof FlowDescriptor f && f.flowType != null)
			return get(f.flowType);
		if (d instanceof CategoryDescriptor c && c.category != null) {
			var icon = categoryIcon(c.categoryType);
			return ImageManager.get(icon);
		}
		return get(d.type);
	}

	public static Image get(Category c) {
		if (c == null)
			return null;
		var icon = categoryIcon(c.modelType);
		return icon == null
			? Icon.FOLDER.get()
			: ImageManager.get(icon);
	}

	public static Image get(Group group) {
		if (group == null)
			return null;
		var icon = icon(group.type);
		return icon == null
			? Icon.FOLDER.get()
			: ImageManager.get(icon);
	}

	public static Image get(ModelType type) {
		var icon = icon(type);
		return icon != null
			? ImageManager.get(icon)
			: null;
	}

	public static Image get(FlowType type) {
		if (type == null)
			return null;
		ModelIcon icon = icon(type);
		return icon == null
			? null
			: ImageManager.get(icon);
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
		return type == null
			? null
			: ImageManager.get(imgPath(type));
	}

	public static ImageDescriptor descriptor(FileType type) {
		return type == null
			? null
			: ImageManager.descriptor(imgPath(type));
	}

	private static String imgPath(FileType type) {
		if (type == null)
			return "file.png";
		return switch (type) {
			case CSV -> "file/csv.png";
			case EXCEL -> "file/excel.png";
			case IMAGE -> "file/image.png";
			case MARKUP -> "file/markup.png";
			case PDF -> "file/pdf.png";
			case POWERPOINT -> "file/powerpoint.png";
			case WORD -> "file/word.png";
			case XML -> "file/xml.png";
			case ZIP -> "file/zip.png";
			case PYTHON -> "python.png";
			case SQL -> "sql.png";
			default -> "file.png";
		};
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
			return Icon.COMMENT.get();
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
		var icon = categoryIcon(type);
		return icon == null
			? Icon.FOLDER.get()
			: ImageManager.get(icon);
	}

	public static Image getForCategory(ModelType type, Overlay overlay) {
		ModelIcon icon = Images.categoryIcon(type);
		if (icon == null)
			return null;
		if (overlay == null)
			return ImageManager.get(icon);
		return ImageManager.get(icon, overlay);
	}

	public static ImageDescriptor descriptor(RefEntity entity) {
		if (entity instanceof Process)
			return descriptor(((Process) entity).processType);
		if (entity instanceof Flow)
			return descriptor(((Flow) entity).flowType);
		return descriptor(ModelType.forModelClass(entity.getClass()));
	}

	public static ImageDescriptor descriptor(Descriptor d) {
		if (d == null || d.type == null)
			return null;
		return switch (d.type) {
			case PROCESS -> descriptor(((ProcessDescriptor) d).processType);
			case FLOW -> descriptor(((FlowDescriptor) d).flowType);
			default -> descriptor(d.type);
		};
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
			return Icon.COMMENT.descriptor();
		return null;
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
		return switch (type) {
			case ACTOR -> ModelIcon.ACTOR_WIZARD;
			case EPD -> ModelIcon.EPD_WIZARD;
			case CURRENCY -> ModelIcon.CURRENCY_WIZARD;
			case FLOW -> ModelIcon.FLOW_WIZARD;
			case FLOW_PROPERTY -> ModelIcon.FLOW_PROPERTY_WIZARD;
			case IMPACT_CATEGORY -> ModelIcon.IMPACT_CATEGORY_WIZARD;
			case IMPACT_METHOD -> ModelIcon.IMPACT_METHOD_WIZARD;
			case LOCATION -> ModelIcon.LOCATION_WIZARD;
			case PARAMETER -> ModelIcon.PARAMETER_WIZARD;
			case PROCESS -> ModelIcon.PROCESS_WIZARD;
			case PRODUCT_SYSTEM -> ModelIcon.PRODUCT_SYSTEM_WIZARD;
			case PROJECT -> ModelIcon.PROJECT_WIZARD;
			case RESULT -> ModelIcon.RESULT_WIZARD;
			case SOCIAL_INDICATOR -> ModelIcon.SOCIAL_INDICATOR_WIZARD;
			case SOURCE -> ModelIcon.SOURCE_WIZARD;
			case UNIT_GROUP -> ModelIcon.UNIT_GROUP_WIZARD;
			case DQ_SYSTEM -> ModelIcon.DQ_SYSTEM_WIZARD;
			default -> null;
		};
	}

	private static ModelIcon icon(GroupType type) {
		if (type == null)
			return null;
		return switch (type) {
			case BACKGROUND_DATA -> ModelIcon.GROUP_BACKGROUND_DATA;
			case INDICATORS -> ModelIcon.GROUP_INDICATORS;
			case MODELS -> ModelIcon.GROUP_MODELS;
		};
	}

	static ModelIcon icon(ModelType type) {
		if (type == null)
			return null;
		return switch (type) {
			case ACTOR -> ModelIcon.ACTOR;
			case EPD -> ModelIcon.EPD;
			case FLOW -> ModelIcon.FLOW;
			case FLOW_PROPERTY -> ModelIcon.FLOW_PROPERTY;
			case IMPACT_METHOD -> ModelIcon.IMPACT_METHOD;
			case IMPACT_CATEGORY -> ModelIcon.IMPACT_CATEGORY;
			case PROCESS -> ModelIcon.PROCESS;
			case PRODUCT_SYSTEM -> ModelIcon.PRODUCT_SYSTEM;
			case PROJECT -> ModelIcon.PROJECT;
			case RESULT -> ModelIcon.RESULT;
			case SOURCE -> ModelIcon.SOURCE;
			case SOCIAL_INDICATOR -> ModelIcon.SOCIAL_INDICATOR;
			case LOCATION -> ModelIcon.LOCATION;
			case PARAMETER -> ModelIcon.PARAMETER;
			case CURRENCY -> ModelIcon.CURRENCY;
			case UNIT_GROUP, UNIT -> ModelIcon.UNIT_GROUP;
			case DQ_SYSTEM -> ModelIcon.DQ_SYSTEM;
			default -> null;
		};
	}

	private static ModelIcon icon(FlowType type) {
		if (type == null)
			return null;
		return switch (type) {
			case ELEMENTARY_FLOW -> ModelIcon.FLOW_ELEMENTARY;
			case PRODUCT_FLOW -> ModelIcon.FLOW_PRODUCT;
			case WASTE_FLOW -> ModelIcon.FLOW_WASTE;
		};
	}

	private static ModelIcon icon(ProcessType type) {
		if (type == null)
			return null;
		return switch (type) {
			case UNIT_PROCESS -> ModelIcon.PROCESS;
			case LCI_RESULT -> ModelIcon.PROCESS_SYSTEM;
		};
	}

	static ModelIcon categoryIcon(ModelType modelType) {
		if (modelType == null)
			return null;
		return switch (modelType) {
			case ACTOR -> ModelIcon.ACTOR_CATEGORY;
			case EPD -> ModelIcon.EPD_CATEGORY;
			case FLOW -> ModelIcon.FLOW_CATEGORY;
			case FLOW_PROPERTY -> ModelIcon.FLOW_PROPERTY_CATEGORY;
			case IMPACT_CATEGORY -> ModelIcon.IMPACT_CATEGORY_CATEGORY;
			case IMPACT_METHOD -> ModelIcon.IMPACT_METHOD_CATEGORY;
			case PROCESS -> ModelIcon.PROCESS_CATEGORY;
			case PRODUCT_SYSTEM -> ModelIcon.PRODUCT_SYSTEM_CATEGORY;
			case PROJECT -> ModelIcon.PROJECT_CATEGORY;
			case RESULT -> ModelIcon.RESULT_CATEGORY;
			case SOURCE -> ModelIcon.SOURCE_CATEGORY;
			case SOCIAL_INDICATOR -> ModelIcon.SOCIAL_INDICATOR_CATEGORY;
			case UNIT_GROUP -> ModelIcon.UNIT_GROUP_CATEGORY;
			case LOCATION -> ModelIcon.LOCATION_CATEGORY;
			case PARAMETER -> ModelIcon.PARAMETER_CATEGORY;
			case CURRENCY -> ModelIcon.CURRENCY_CATEGORY;
			case DQ_SYSTEM -> ModelIcon.DQ_SYSTEM_CATEGORY;
			default -> null;
		};
	}

	/**
	 * Returns the shared image with the given name from the Eclipse platform. See
	 * ISharedImages for the image names.
	 */
	public static Image platformImage(String name) {
		return PlatformUI.getWorkbench().getSharedImages().getImage(name);
	}

}
