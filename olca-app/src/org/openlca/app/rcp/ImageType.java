package org.openlca.app.rcp;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

import com.google.common.io.Files;

public enum ImageType {

	ACCEPT("graphical/accept.png"),

	ACTOR("model/actor.png"),

	ACTOR_CATEGORY("category/actor.png"),

	ACTOR_WIZARD("wizard/actor.png"),

	ADD("add.png"),

	ADD_DISABLED("add_disabled.png"),

	BACKGROUND_DATA_GROUP("model/background_data_group.png"),

	BUILD_SUPPLY_CHAIN("graphical/build_supply_chain.gif"),

	CALCULATE("calculation/calculate.png"),

	CALCULATE_COSTS("calculation/calculate_costs.png"),

	CALCULATION_WIZARD("wizard/calculation.gif"),

	CHANGE("change.gif"),

	CHART("chart.png"),

	CHECK_FALSE("check_false.gif"),

	CHECK_TRUE("check_true.gif"),

	COLLAPSE("collapse.png"),

	COMMIT("cloud/commit.png"),

	CONNECT("connect.png"),

	COPY_ALL_CHANGES("cloud/copy_all.png"),

	COPY_SELECTED_CHANGE("cloud/copy_selected.png"),

	CURRENCY("model/currency.png"),

	CURRENCY_CATEGORY("category/currency.png"),

	CURRENCY_WIZARD("wizard/currency.png"),

	DATABASE("model/database.png"),

	DATABASE_DISABLED("model/database_disabled.png"),

	DATABASE_IO("database_io.gif"),

	DATABASE_WIZARD("wizard/database.png"),

	DELETE("delete.gif"),

	DELETE_DISABLED("delete_disabled.gif"),

	DISCONNECT("disconnect.png"),

	DOWN("down.png"),

	EDIT("edit.png"),

	ERROR("error.png"),

	EXCHANGE_BG_LEFT("graphical/exchange_bg_left.jpg"),

	EXCHANGE_BG_MIDDLE("graphical/exchange_bg_middle.jpg"),

	EXCHANGE_BG_RIGHT("graphical/exchange_bg_right.jpg"),

	EXPAND("expand.png"),

	EXPORT("io/export.png"),

	EXPRESSION("expression.gif"),

	EXTENSION("extension.gif"),

	FILE("file.gif"),

	FILE_CSV("file/csv.gif"),

	FILE_EXCEL("file/excel.png"),

	FILE_IMAGE("file/image.png"),

	FILE_MARKUP("file/markup.png"),

	FILE_PDF("file/pdf.png"),

	FILE_POWERPOINT("file/powerpoint.png"),

	FILE_WORD("file/word.png"),

	FILE_XML("file/xml.png"),

	FILE_ZIP("file/zip.gif"),

	FIREFOX("firefox.png"),

	FLOW("model/flow.png"),

	FLOW_CATEGORY("category/flow.png"),

	FLOW_ELEMENTARY("model/flow_elementary.png"),

	FLOW_PRODUCT("model/flow_product.png"),

	FLOW_PROPERTY("model/flow_property.png"),

	FLOW_PROPERTY_CATEGORY("category/flow_property.png"),

	FLOW_PROPERTY_WIZARD("wizard/flow_property.png"),

	FLOW_WASTE("model/flow_waste.png"),

	FLOW_WIZARD("wizard/flow.png"),

	FOLDER("folder.gif"),

	FOLDER_BLUE("folder_blue.png"),

	FOLDER_OPEN("folder_open.gif"),

	FORMULA("formula.png"),

	HELP("help.gif"),

	HOME("home.png"),

	IMPACT_METHOD("model/impact_method.png"),

	IMPACT_METHOD_CATEGORY("category/impact_method.png"),

	IMPACT_METHOD_WIZARD("wizard/impact_method.png"),

	IMPORT("import.png"),

	IMPORT_ZIP_WIZARD("wizard/zip.png"),

	INFO("info.gif"),

	INPUT("model/input.png"),

	INVENTORY_GROUP("model/inventory_group.png"),

	JAVASCRIPT("javascript.gif"),

	LAYOUT("graphical/layout.gif"),

	LINK("link.png"),

	LOCATION("model/location.png"),

	LOCATION_CATEGORY("category/location.png"),

	LOCATION_WIZARD("wizard/location.png"),

	LOGO("plugin/logo_32_32bit.png"),

	MAXIMIZE("graphical/maxmize.gif"),

	MINIATURE_VIEW("graphical/miniature_view.gif"),

	MINIMIZE("graphical/minimize.gif"),

	MINUS("graphical/minus.gif"),

	MODELS_GROUP("model/models_group.png"),

	NEXT_CHANGE("cloud/next_change.png"),

	NEW_WIZARD("wizard/new.png"),

	NUMBER("number.png"),

	OK("ok.png"),

	OUTLINE("graphical/outline.gif"),

	OUTPUT("model/output.png"),

	OVERLAY_ADDED("overlay/cloud/added.png"),

	OVERLAY_ADD_TO_LOCAL("overlay/cloud/add_local.png"),

	OVERLAY_ADD_TO_REMOTE("overlay/cloud/add_remote.png"),

	OVERLAY_CONFLICT("overlay/cloud/conflict.png"),

	OVERLAY_DELETED("overlay/cloud/deleted.png"),

	OVERLAY_DELETE_FROM_LOCAL("overlay/cloud/delete_local.png"),

	OVERLAY_DELETE_FROM_REMOTE("overlay/cloud/delete_remote.png"),

	OVERLAY_MERGED("overlay/cloud/merged.png"),

	OVERLAY_MODIFY_IN_LOCAL("overlay/cloud/modify_local.png"),

	OVERLAY_MODIFY_IN_REMOTE("overlay/cloud/modify_remote.png"),

	OVERLAY_NEW("overlay/new.png"),

	PARAMETER("model/parameter.png"),

	PARAMETER_CATEGORY("category/parameter.png"),

	PARAMETER_WIZARD("wizard/parameter.png"),

	PLUS("graphical/plus.gif"),

	PREVIOUS_CHANGE("cloud/previous_change.png"),

	PROCESS("model/process.png"),

	PROCESS_BG("graphical/process_bg.jpg"),

	PROCESS_BG_LCI("graphical/process_bg_lci.jpg"),

	PROCESS_BG_MARKED("graphical/process_bg_marked.jpg"),

	PROCESS_CATEGORY("category/process.png"),

	PROCESS_SYSTEM("model/process_system.png"),

	PROCESS_WIZARD("wizard/process.png"),

	PRODUCT_SYSTEM("model/product_system.png"),

	PRODUCT_SYSTEM_CATEGORY("category/product_system.png"),

	PRODUCT_SYSTEM_WIZARD("wizard/product_system.png"),

	PROJECT("model/project.png"),

	PROJECT_CATEGORY("category/project.png"),

	PROJECT_WIZARD("wizard/project.png"),

	PYTHON("python.gif"),

	REFRESH("refresh.png"),

	RESET_ALL_CHANGES("cloud/reset_all.png"),

	RESET_SELECTED_CHANGE("cloud/reset_selected.png"),

	RUN("run.gif"),

	SANKEY_OPTIONS("graphical/sankey_options.gif"),

	SAVE_AS_IMAGE("save_as_image.png"),

	SEARCH("search.png"),

	SIMULATE("calculation/simulate.png"),

	SOCIAL_INDICATOR("model/social_indicator.png"),

	SOCIAL_INDICATOR_CATEGORY("category/social_indicator.png"),

	SOCIAL_INDICATOR_WIZARD("wizard/social_indicator.png"),

	SOURCE("model/source.png"),

	SOURCE_CATEGORY("category/source.png"),

	SOURCE_WIZARD("wizard/source.png"),

	SQL("sql.gif"),

	UNIT_GROUP("model/unit_group.png"),

	UNIT_GROUP_CATEGORY("category/unit_group.png"),

	UNIT_GROUP_WIZARD("wizard/unit_group.png"),

	UP("up.png"),

	UP_DISABLED("up_disabled.png"),

	UP_DOUBLE("up_double.png"),

	UP_DOUBLE_DISABLED("up_double_disabled.png"),

	WARNING("warning.png");

	private final String fileName;

	private ImageType(String fileName) {
		this.fileName = fileName;
	}

	Image createImage() {
		return RcpActivator.getImageDescriptor(getPath()).createImage();
	}

	public Image get() {
		return ImageManager.getImage(this);
	}

	public ImageDescriptor getDescriptor() {
		return ImageManager.getImageDescriptor(this);
	}

	public String getPath() {
		return "icons/" + this.fileName;
	}

	public static ImageType forFile(String fileName) {
		if (fileName == null)
			return FILE;
		String extension = Files.getFileExtension(fileName);
		if (extension == null)
			return FILE;
		switch (extension) {
		case "pdf":
			return FILE_PDF;
		case "doc":
		case "docx":
		case "odt":
			return FILE_WORD;
		case "xls":
		case "xlsx":
		case "ods":
		case "csv":
			return FILE_EXCEL;
		case "png":
		case "jpg":
		case "gif":
			return FILE_IMAGE;
		case "ppt":
		case "pptx":
		case "odp":
			return FILE_POWERPOINT;
		case "xml":
		case "html":
		case "spold":
		case "htm":
		case "xhtml":
			return FILE_MARKUP;
		default:
			return FILE;
		}
	}

	/**
	 * Returns the shared image descriptor with the given name from the Eclipse
	 * platform. See ISharedImages for the image names.
	 */
	public static ImageDescriptor getPlatformDescriptor(String name) {
		return PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(name);
	}

	/**
	 * Returns the shared image with the given name from the Eclipse platform.
	 * See ISharedImages for the image names.
	 */
	public static Image getPlatformImage(String name) {
		return PlatformUI.getWorkbench().getSharedImages().getImage(name);
	}

	public static Image getNewImage(ImageType type) {
		return ImageManager.getImageWithOverlay(type, OVERLAY_NEW);
	}

	public static ImageDescriptor getNewDescriptor(ImageType type) {
		// TODO fix this, it does not work
		return ImageManager.getImageDescriptorWithOverlay(type, OVERLAY_NEW);
	}

}
