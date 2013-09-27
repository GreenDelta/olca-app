package org.openlca.app.resources;

import java.io.File;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.RcpActivator;

public enum ImageType implements IImageType {

	ACTOR_CATEGORY_ICON("folder_user.png"),

	ACTOR_ICON("actor_obj.png"),

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

	DISCONNECT_ICON("disconnect.png"),

	ERROR_ICON("error.gif"),

	EXCEL_ICON("excel_obj.png"),

	EXCHANGE_BG_LEFT("left.jpg"),

	EXCHANGE_BG_MIDDLE("middle.jpg"),

	EXCHANGE_BG_RIGHT("right.jpg"),

	EXPAND_ICON("expandall.gif"),

	EXPRESSION_ICON("expression_obj.gif"),

	EXPRESSION_ICON_DIS("expression_obj_dis.gif"),

	FILE_ICON("file_16.gif"),

	FLOW_CATEGORY_ICON("folder_flow.png"),

	FLOW_ICON("flow_obj.gif"),

	FLOW_PRODUCT("flow_product_16.png"),

	FLOW_PROPERTY_CATEGORY_ICON("folder_fp.png"),

	FLOW_PROPERTY_ICON("flowprop_obj.gif"),

	FLOW_SUBSTANCE("flow_substance_16.png"),

	FLOW_WASTE("flow_waste_16.png"),

	COLLAPSE_ICON("collapse_all.png"),

	FOLDER_EXPR("folder_expr.png"),

	FOLDER_ICON_BLUE("16x16_folder_blue.png"),

	FOLDER_ICON_OPEN("16x16_folder_open.gif"),

	FORMULA_ICON("formula_16.png"),

	HIDE_ICON("hide.gif"),

	ILCD_ICON("ilcd16.png"),

	IMPORT_ZIP_WIZARD("import_zip_wizard.png"),

	INFO_ICON("info_tsk.gif"),

	INPUT_ICON("input_16.png"),

	LAYOUT_ICON("layout.gif"),

	LCIA_CATEGORY_ICON("folder_wa.png"),

	LCIA_CATEGORY_ICON_DIS("folder_wa_dis.png"),

	LCIA_ICON("LCIA_obj.gif"),

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

	MINIMAL_TREE_LAYOUT_ICON("layout.gif"),

	MINIMIZE_ICON("minAll.gif"),

	MINUS_ICON("del_stat.gif"),

	MODEL_ICON("model.gif"),

	NEW_DB_ICON("db_obj_new.gif"),

	NEW_WIZ_ACTOR("new_wiz_actor.png"),

	NEW_WIZ_DATABASE("new_wiz_database.png"),

	NEW_WIZ_FLOW("new_wiz_flow.png"),

	NEW_WIZ_METHOD("new_wiz_method.png"),

	NEW_WIZ_PROCESS("new_wiz_process.png"),

	NEW_WIZ_PRODUCT_SYSTEM("new_wiz_product_system.png"),

	NEW_WIZ_PROJECT("new_wiz_project.png"),

	NEW_WIZ_PROPERTY("new_wiz_property.png"),

	NEW_WIZ_SOURCE("new_wiz_source.png"),

	NEW_WIZ_UNIT("new_wiz_unit.png"),

	NEW_WIZARD("new_wiz.png"),

	NOT_OK_ICON("not_ok_16.png"),

	NUMBER_ICON("number_16.png"),

	OK_CHECK_ICON("ok_check_16.png"),

	OUTPUT_ICON("output_16.png"),

	PLUS_ICON("add_stat.gif"),

	PROCESS_BG("process_bg.jpg"),

	PROCESS_BG_LCI("process_bg_lci.jpg"),

	PROCESS_BG_MARKED("process_bg_marked.jpg"),

	PROCESS_CATEGORY_ICON("folder_process.png"),

	PROCESS_CONNECTED("connected.png"),

	PROCESS_EXISTING("existing.png"),

	PROCESS_ICON("proc_obj.gif"),

	PRODUCT_SYSTEM_CATEGORY_ICON("folder_prodsystem.png"),

	PRODUCT_SYSTEM_ICON("system_obj.gif"),

	PROJECT_CATEGORY_ICON("folder_proj.png"),

	PROJECT_ICON("project_obj.png"),

	REFRESH_ICON("refresh_16.png"),

	SANKEY_OPTIONS_ICON("sankey_options.gif"),

	SAVE_AS_IMAGE_ICON("image_save.png"),

	SEARCH_ICON("search_obj.png"),

	SIMULATE_16("simulate_16.png"),

	SINGLE_SCORE_ICON("singlescore.png"),

	SOURCE_CATEGORY_ICON("folder_source.png"),

	SOURCE_ICON("source_obj.png"),

	SP_ICON("sp.png"),

	SWITCH_ICON("switch_view_mode.gif"),

	TABLE_ICON("table.png"),

	TEXT_SIZE_BIG("text_allcaps_24.png"),

	TEXT_SIZE_MEDIUM("text_allcaps_20.png"),

	TEXT_SIZE_SMALL("text_allcaps_16.png"),

	TREE_LAYOUT_ICON("layout_treelayout.gif"),

	UNIT_GROUP_CATEGORY_ICON("folder_unit.png"),

	UNIT_GROUP_ICON("unitgroup_obj.gif"),

	UNSELECT_ICON("unselectAll.gif"),

	WARNING_ICON("warning.gif"),

	WIZ_CALCULATION("wiz_calculation.gif"),

	XML_ICON("xml_16.gif"),

	ZIP_ICON("zip.gif");

	private final String fileName;

	private ImageType(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public Image createImage() {
		return RcpActivator.getImageDescriptor(getPath()).createImage();
	}

	@Override
	public Image get() {
		return ImageManager.getImage(this);
	}

	public ImageDescriptor getDescriptor() {
		return ImageManager.getImageDescriptor(this);
	}

	@Override
	public String getPath() {
		return File.separator + "icons" + File.separator + this.fileName;
	}

}
