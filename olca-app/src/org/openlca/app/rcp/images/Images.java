package org.openlca.app.rcp.images;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.navigation.elements.Group;
import org.openlca.app.navigation.elements.GroupType;
import org.openlca.app.util.FileType;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.Category;
import org.openlca.core.model.Direction;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.RefEntity;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Source;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class Images {

	public static Image get(EnviFlow f) {
		if (f == null)
			return null;
		return f.isVirtual() && f.wrapped() != null
				? get(f.wrapped())
				: get(f.flow());
	}

	public static Image get(TechFlow techFlow) {
		return techFlow != null
				? get(techFlow.provider())
				: null;
	}

	public static Image get(RefEntity e) {
		if (e instanceof Source source) {
			if (source.externalFile != null)
				return get(FileType.forName(source.externalFile));
		}
		var icon = icon(e);
		if (icon == null)
			return null;
		return e instanceof RootEntity root && root.isFromLibrary()
				? ImageManager.get(icon, Overlay.LIBRARY)
				: ImageManager.get(icon);
	}

	public static Image get(Descriptor d) {
		var icon = icon(d);
		if (icon == null)
			return null;
		return d.isFromLibrary()
				? ImageManager.get(icon, Overlay.LIBRARY)
				: ImageManager.get(icon);
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
		var icon = icon(group.type);
		return icon != null
				? ImageManager.get(icon)
				: Icon.FOLDER.get();
	}

	public static Image get(ModelType type) {
		return imageOf(icon(type));
	}

	public static Image get(FlowType type) {
		return imageOf(icon(type));
	}

	public static Image get(ProcessType type) {
		return imageOf(icon(type));
	}

	public static Image get(GroupType type) {
		return imageOf(icon(type));
	}

	private static Image imageOf(ModelIcon icon) {
		return icon != null
				? ImageManager.get(icon)
				: null;
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

	public static Image library(Overlay overlay) {
		var icon = Icon.LIBRARY;
		return overlay == null
				? icon.get()
				: ImageManager.get(icon, overlay);		
	}

	public static Image get(ModelType type, Overlay overlay) {
		var icon = icon(type);
		if (icon == null)
			return null;
		return overlay == null
				? ImageManager.get(icon)
				: ImageManager.get(icon, overlay);
	}

	public static Image get(Descriptor d, Overlay overlay) {
		var icon = icon(d);
		if (icon == null)
			return null;
		return overlay != null
				? ImageManager.get(icon, overlay)
				: ImageManager.get(icon);
	}

	public static Image get(Boolean value) {
		return value == null || !value
				? Icon.CHECK_FALSE.get()
				: Icon.CHECK_TRUE.get();
	}

	public static Image get(Comments comments, String path) {
		return comments != null && comments.hasPath(path)
				? Icon.COMMENT.get()
				: null;
	}

	public static Image getForCategory(ModelType type) {
		var icon = categoryIcon(type);
		return icon == null
				? Icon.FOLDER.get()
				: ImageManager.get(icon);
	}

	public static ImageDescriptor descriptor(RefEntity entity) {
		var icon = icon(entity);
		if (icon == null)
			return null;
		return entity instanceof RootEntity root && root.isFromLibrary()
				? ImageManager.descriptor(icon, Overlay.LIBRARY)
				: ImageManager.descriptor(icon);
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
		var icon = categoryIcon(category.modelType);
		return icon != null
				? ImageManager.descriptor(icon)
				: Icon.FOLDER.descriptor();
	}

	public static ImageDescriptor descriptor(Group group) {
		if (group == null)
			return null;
		var icon = icon(group.type);
		return icon != null
				? ImageManager.descriptor(icon)
				: Icon.FOLDER.descriptor();
	}

	public static ImageDescriptor descriptor(ModelType type) {
		return descriptorOf(icon(type));
	}

	public static ImageDescriptor descriptor(FlowType type) {
		return descriptorOf(icon(type));
	}

	public static ImageDescriptor descriptor(ProcessType type) {
		return descriptorOf(icon(type));
	}

	private static ImageDescriptor descriptorOf(ModelIcon icon) {
		return icon != null
				? ImageManager.descriptor(icon)
				: null;
	}

	public static ImageDescriptor descriptor(GroupType type) {
		ModelIcon icon = icon(type);
		if (icon == null)
			return Icon.FOLDER.descriptor();
		return ImageManager.descriptor(icon);
	}

	public static ImageDescriptor descriptor(ModelType type, Overlay overlay) {
		var icon = Images.icon(type);
		if (icon == null)
			return null;
		return overlay != null
				? ImageManager.descriptor(icon, overlay)
				: ImageManager.descriptor(icon);
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

	public static Image licensedLibrary(boolean isValid) {
		var overlay = isValid ? Overlay.VALID : Overlay.INVALID;
		return ImageManager.get(Icon.LIBRARY, overlay);
	}

	private static ModelIcon icon(RefEntity entity) {
		if (entity instanceof Process p) {
			var qRef = p.quantitativeReference;
			var flowType = qRef != null && qRef.flow != null
					? qRef.flow.flowType
					: null;
			if (p.processType == ProcessType.LCI_RESULT) {
				return flowType == FlowType.WASTE_FLOW
						? ModelIcon.PROCESS_SYSTEM_WASTE
						: ModelIcon.PROCESS_SYSTEM_PROD;
			}
			return flowType == FlowType.WASTE_FLOW
					? ModelIcon.PROCESS_WASTE
					: ModelIcon.PROCESS_PROD;
		}

		if (entity instanceof Flow flow)
			return icon(flow.flowType);
		if (entity instanceof Unit)
			return icon(ModelType.UNIT_GROUP);
		if (entity instanceof ImpactCategory impact)
			return impact.direction == Direction.INPUT
					? ModelIcon.IMPACT_CATEGORY_IN
					: ModelIcon.IMPACT_CATEGORY_OUT;

		if (entity instanceof RootEntity re)
			return icon(ModelType.of(re));

		return null;
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

	private static ModelIcon icon(Descriptor d) {
		if (d == null)
			return null;

		if (d instanceof ProcessDescriptor p) {
			if (p.processType == ProcessType.LCI_RESULT)
				return p.flowType == FlowType.WASTE_FLOW
						? ModelIcon.PROCESS_SYSTEM_WASTE
						: ModelIcon.PROCESS_SYSTEM_PROD;
			return p.flowType == FlowType.WASTE_FLOW
					? ModelIcon.PROCESS_WASTE
					: ModelIcon.PROCESS_PROD;
		}

		if (d instanceof FlowDescriptor f && f.flowType != null)
			return icon(f.flowType);
		if (d instanceof CategoryDescriptor c && c.category != null)
			return categoryIcon(c.categoryType);

		if (d instanceof ImpactDescriptor i) {
			return i.direction == Direction.INPUT
					? ModelIcon.IMPACT_CATEGORY_IN
					: ModelIcon.IMPACT_CATEGORY_OUT;
		}
		return icon(d.type);
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

	private static ModelIcon icon(ModelType type) {
		if (type == null)
			return null;
		return switch (type) {
			case ACTOR -> ModelIcon.ACTOR;
			case EPD -> ModelIcon.EPD;
			case FLOW -> ModelIcon.FLOW;
			case FLOW_PROPERTY -> ModelIcon.FLOW_PROPERTY;
			case IMPACT_METHOD -> ModelIcon.IMPACT_METHOD;
			case IMPACT_CATEGORY -> ModelIcon.IMPACT_CATEGORY_OUT;
			case PROCESS -> ModelIcon.PROCESS_GENERIC;
			case PRODUCT_SYSTEM -> ModelIcon.PRODUCT_SYSTEM;
			case PROJECT -> ModelIcon.PROJECT;
			case RESULT -> ModelIcon.RESULT;
			case SOURCE -> ModelIcon.SOURCE;
			case SOCIAL_INDICATOR -> ModelIcon.SOCIAL_INDICATOR;
			case LOCATION -> ModelIcon.LOCATION;
			case PARAMETER -> ModelIcon.PARAMETER;
			case CURRENCY -> ModelIcon.CURRENCY;
			case UNIT_GROUP -> ModelIcon.UNIT_GROUP;
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
			case UNIT_PROCESS -> ModelIcon.PROCESS_PROD;
			case LCI_RESULT -> ModelIcon.PROCESS_SYSTEM_PROD;
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
