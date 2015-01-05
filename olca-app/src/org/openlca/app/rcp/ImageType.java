package org.openlca.app.rcp;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

import com.google.common.io.Files;

public enum ImageType {

	ACCEPT_ICON("accept.png"),

	ACTOR_CATEGORY_ICON("folder_user.png"),

	ACTOR_ICON("actor_obj.png"),

	ACTOR_ICON_NEW("actor_obj_new.png"),

	ADD_ICON("add.png"),

	ADD_ICON_DISABLED("add_dis.png"),

	ANALYZE_DISABLED_ICON("analyze_dis.gif"),

	ANALYZE_ICON("analyze.gif"),

	BUILD_SUPPLY_CHAIN_ICON("chain_complete.gif"),

	CALCULATE_ICON("calculate.gif"),

	CHANGE_ICON("change.gif"),

	CHANGE_ICON_DISABLED("change_dis.gif"),

	CHART_ICON("chart.png"),

	CHECK_FALSE("check_false.gif"),

	CHECK_TRUE("check_true.gif"),

	CONNECT_ICON("connect.png"),

	COST_CALC_ICON("cost_calc_icon.png"),

	DB_ICON("db_obj.gif"),

	DB_ICON_DIS("db_obj_dis.png"),

	DB_IO("db_io_16.gif"),

	DB_WIZARD("db_wizard.gif"),

	DELETE_ICON("delete.gif"),

	DELETE_ICON_DISABLED("delete_dis.gif"),

	DENY_ICON("deny.png"),

	DISCONNECT_ICON("disconnect.png"),

	DOWN_16("down_16.png"),

	DOWN_DIS_16("down_dis_16.png"),

	EDIT_16("edit_16.png"),

	ERROR_ICON("error.gif"),

	EXCHANGE_BG_LEFT("left.jpg"),

	EXCHANGE_BG_MIDDLE("middle.jpg"),

	EXCHANGE_BG_RIGHT("right.jpg"),

	EXPAND_ICON("expandall.gif"),

	EXPORT_ICON("export_wiz_16x16.gif"),

	EXPRESSION_ICON("expression_obj.gif"),

	EXPRESSION_ICON_DIS("expression_obj_dis.gif"),

	EXTENSION_ICON("extension_16.gif"),

	FILE_SMALL("file_16.gif"),

	FILE_EXCEL_SMALL("file_excel_16.png"),

	FILE_IMAGE_SMALL("file_image_16.png"),

	FILE_MARKUP_SMALL("file_markup_16.png"),

	FILE_PDF_SMALL("file_pdf_16.png"),

	FILE_POWERPOINT_SMALL("file_powerpoint_16.png"),

	FILE_WORD_SMALL("file_word_16.png"),

	FIREFOX_ICON("firefox_16.png"),

	FLOW_CATEGORY_ICON("folder_flow.png"),

	FLOW_ICON("flow_obj.gif"),

	FLOW_ICON_NEW("flow_obj_new.gif"),

	FLOW_PRODUCT("flow_product_16.png"),

	FLOW_PROPERTY_CATEGORY_ICON("folder_fp.png"),

	FLOW_PROPERTY_ICON("flowprop_obj.gif"),

	FLOW_PROPERTY_ICON_NEW("flowprop_obj_new.gif"),

	FLOW_SUBSTANCE("flow_substance_16.png"),

	FLOW_WASTE("flow_waste_16.png"),

	FOLDER_SMALL("folder_16.gif"),

	COLLAPSE_ICON("collapse_all.png"),

	FOLDER_EXPR("folder_expr.png"),

	FOLDER_ICON_BLUE("16x16_folder_blue.png"),

	FOLDER_ICON_OPEN("16x16_folder_open.gif"),

	FORMULA_ICON("formula_16.png"),

	HIDE_ICON("hide.gif"),

	HOME_ICON("home_16.gif"),

	ILCD_ICON("ilcd16.png"),

	IMPORT_ICON("import_wiz_16x16.gif"),

	IMPORT_ZIP_WIZARD("import_zip_wizard.png"),

	HELP_ICON("help_16x16.gif"),

	INFO_ICON("info_tsk.gif"),

	INPUT_ICON("input_16.png"),

	JAVASCRIPT_ICON("javascript_16.gif"),

	LAYOUT_ICON("layout.gif"),

	LCIA_CATEGORY_ICON("folder_wa.png"),

	LCIA_CATEGORY_ICON_DIS("folder_wa_dis.png"),

	LCIA_ICON("LCIA_obj.gif"),

	LCIA_ICON_NEW("LCIA_obj_new.gif"),

	LINK_16_BLUE("16x16_link_blue.png"),

	LOAD_ICON("load_obj.gif"),

	LOGO_128_32("logo_128_32bit.png"),

	LOGO_16_32("logo_16_32bit.png"),

	LOGO_255_32("logo_255_32bit.png"),

	LOGO_32_32("logo_32_32bit.png"),

	LOGO_64_32("logo_64_32bit.png"),

	MATRIX_ICON("table_16.gif"),

	MAXIMIZE_ICON("maxAll.gif"),

	MINI_VIEW_ICON("miniview.gif"),

	MINIMIZE_ICON("minAll.gif"),

	MINUS_ICON("del_stat.gif"),

	MODEL_ICON("model.gif"),

	NEW_DB_ICON("db_obj_new.gif"),

	NEW_WIZ_ACTOR("new_wiz_actor.png"),

	NEW_WIZ_DATABASE("new_wiz_database.gif"),

	NEW_WIZ_FLOW("new_wiz_flow.png"),

	NEW_WIZ_METHOD("new_wiz_method.gif"),

	NEW_WIZ_PROCESS("new_wiz_process.png"),

	NEW_WIZ_PRODUCT_SYSTEM("new_wiz_product_system.png"),

	NEW_WIZ_PROJECT("new_wiz_project.png"),

	NEW_WIZ_PROPERTY("new_wiz_property.png"),

	NEW_WIZ_SOURCE("new_wiz_source.gif"),

	NEW_WIZ_UNIT("new_wiz_unit.png"),

	NEW_WIZARD("new_wiz.png"),

	NOT_OK_ICON("not_ok_16.png"),

	NUMBER_ICON("number_16.png"),

	OK_CHECK_ICON("ok_check_16.png"),

	OUTLINE_ICON("outline_co.gif"),

	OUTPUT_ICON("output_16.png"),

	PLUS_ICON("add_stat.gif"),

	PROCESS_BG("process_bg.jpg"),

	PROCESS_BG_LCI("process_bg_lci.jpg"),

	PROCESS_BG_MARKED("process_bg_marked.jpg"),

	PROCESS_CATEGORY_ICON("folder_process.png"),

	PROCESS_CONNECTED("connected.png"),

	PROCESS_EXISTING("existing.png"),

	PROCESS_ICON("proc_obj.gif"),

	PROCESS_ICON_NEW("proc_obj_new.gif"),

	PRODUCT_SYSTEM_CATEGORY_ICON("folder_prodsystem.png"),

	PRODUCT_SYSTEM_ICON("system_obj.gif"),

	PRODUCT_SYSTEM_ICON_NEW("chart_organisation_new.png"),

	PROJECT_CATEGORY_ICON("folder_proj.png"),

	PROJECT_ICON("project_obj.png"),

	PROJECT_ICON_NEW("project_obj_new.png"),

	PYTHON_ICON("python_16.gif"),

	REFRESH_ICON("refresh_16.png"),

	RUN_SMALL("run_16.gif"),

	SANKEY_OPTIONS_ICON("sankey_options.gif"),

	SAVE_AS_IMAGE_ICON("image_save.png"),

	SEARCH_ICON("search_obj.png"),

	SIMULATE_16("simulate_16.png"),

	SINGLE_SCORE_ICON("singlescore.png"),

	SOURCE_CATEGORY_ICON("folder_source.png"),

	SOURCE_ICON("source_obj.png"),

	SOURCE_ICON_NEW("source_obj_new.png"),

	SP_ICON("sp.png"),

	SQL_ICON("sql_16.gif"),

	SWITCH_ICON("switch_view_mode.gif"),

	TABLE_ICON("table.png"),

	TEXT_SIZE_BIG("text_allcaps_24.png"),

	TEXT_SIZE_MEDIUM("text_allcaps_20.png"),

	TEXT_SIZE_SMALL("text_allcaps_16.png"),

	UP_16("up_16.png"),

	UP_DIS_16("up_dis_16.png"),

	UP_DOUBLE_16("up_double_16.png"),

	UP_DOUBLE_DIS_16("up_double_dis_16.png"),

	UNIT_GROUP_CATEGORY_ICON("folder_unit.png"),

	UNIT_GROUP_ICON("unitgroup_obj.gif"),

	UNIT_GROUP_ICON_NEW("unitgroup_obj_new.gif"),

	UNSELECT_ICON("unselectAll.gif"),

	WARNING_ICON("warning.gif"),

	WIZ_CALCULATION("wiz_calculation.gif"),

	XML_ICON("xml_16.gif"),

	ZIP_ICON("zip.gif");

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
			return FILE_SMALL;
		String extension = Files.getFileExtension(fileName);
		if (extension == null)
			return FILE_SMALL;
		switch (extension) {
		case "pdf":
			return FILE_PDF_SMALL;
		case "doc":
		case "docx":
		case "odt":
			return FILE_WORD_SMALL;
		case "xls":
		case "xlsx":
		case "ods":
		case "csv":
			return FILE_EXCEL_SMALL;
		case "png":
		case "jpg":
		case "gif":
			return FILE_IMAGE_SMALL;
		case "ppt":
		case "pptx":
		case "odp":
			return FILE_POWERPOINT_SMALL;
		case "xml":
		case "html":
		case "spold":
		case "htm":
		case "xhtml":
			return FILE_MARKUP_SMALL;
		default:
			return FILE_SMALL;
		}
	}

	/**
	 * Returns the shared image descriptor with the given name from the Eclipse
	 * platform. See ISharedImages for the image names.
	 */
	public static ImageDescriptor getPlatformDescriptor(String name) {
		return PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(name);
	}

	/**
	 * Returns the shared image with the given name from the Eclipse platform.
	 * See ISharedImages for the image names.
	 */
	public static Image getPlatformImage(String name) {
		return PlatformUI.getWorkbench()
				.getSharedImages().getImage(name);
	}
}
