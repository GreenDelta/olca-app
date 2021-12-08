package org.openlca.app.rcp.images;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.openlca.swt.material.icons.MaterialIcon;

public enum Icon {

	ACCEPT("graphical/accept.png"),
	ADD("add.png"),
	ADD_DISABLED("add_disabled.png"),
	ANALYSIS_RESULT("analysis.png"),

	BUILD_SUPPLY_CHAIN("graphical/build_supply_chain.gif"),
	BUILDING("building.png"),

	CALCULATE_COSTS("calculation/calculate_costs.png"),
	CALCULATION_WIZARD("wizard/calculation.gif"),
	CHANGE("change.png"),
	CHART("chart.png"),
	CHECK_FALSE("check_false.gif"),
	CHECK_TRUE("check_true.gif"),
	COLLAPSE("collapse.png"),
	COMMIT("cloud/commit.png"),
	CONNECT("connect.png"),
	COPY("copy.png"),
	COPY_ALL_CHANGES("cloud/copy_all.png"),
	COPY_SELECTED_CHANGE("cloud/copy_selected.png"),
	CLOUD_LOGO("cloud/logo.png"),
	CUT("cut.png"),

	DATABASE("model/database.png"),
	DATABASE_DISABLED("model/database_disabled.png"),
	DATABASE_IMPORT("database_import.png"),
	DATABASE_EXPORT("database_export.png"),
	DATABASE_WIZARD("wizard/database.png"),
	DELETE("delete.png"),
	DELETE_DISABLED("delete_disabled.png"),
	DISCONNECT("disconnect.png"),
	DOWN("down.png"),

	EC3_WIZARD("wizard/ec3.png"),
	EDIT("edit.png"),
	ERROR("error.png"),
	EXCHANGE_BG_LEFT("graphical/exchange_bg_left.jpg"),
	EXCHANGE_BG_MIDDLE("graphical/exchange_bg_middle.jpg"),
	EXCHANGE_BG_RIGHT("graphical/exchange_bg_right.jpg"),
	EXPAND("expand.png"),
	EXPORT("export.png"),
	EXPRESSION("expression.png"),
	EXTENSION("extension.gif"),

	FILE("file.png"),
	FIREFOX("firefox.png"),
	FOLDER("folder.png"),
	FOLDER_BLUE("folder_blue.png"),
	FOLDER_OPEN("folder_open.png"),
	FORMULA("formula.png"),

	GRAPH_PROCESS_PRODUCTION("graphical/process_production.png"),

	HELP("help.png"),
	HOME("home.png"),

	IMPORT("import.png"),
	IMPORT_ZIP_WIZARD("wizard/zip.png"),
	INFO("info.png"),
	INPUT("model/input.png"),

	JAVASCRIPT("javascript.png"),

	LAYOUT("graphical/layout.png"),
	LIBRARY("library.png"),
	LINK("link.png"),
	LOCK("lock.png"),
	LOGO("plugin/logo_32_32bit.png"),

	MANAGE_PLUGINS("manage_plugins.png"),
	MAP("map.png"),
	MAXIMIZE("graphical/maximize.png"),
	MINIATURE_VIEW("graphical/miniature_view.png"),
	MINIMIZE("graphical/minimize.png"),
	MINUS("graphical/minus.gif"),

	NEXT_CHANGE("cloud/next_change.png"),
	NEW_WIZARD("wizard/new.png"),
	NUMBER("number.png"),

	OUTLINE("graphical/outline.png"),
	OUTPUT("model/output.png"),

	PASTE("paste.png"),
	PLUS("graphical/plus.gif"),
	PREFERENCES("preferences.png"),
	PREVIOUS_CHANGE("cloud/previous_change.png"),
	PROCESS_BG("graphical/process_bg.jpg"),
	PROCESS_BG_LCI("graphical/process_bg_lci.jpg"),
	PROCESS_BG_SYS("graphical/process_bg_sys.jpg"),
	PROCESS_BG_MARKED("graphical/process_bg_marked.jpg"),
	PROCESS_BG_LIB("graphical/process_bg_lib.jpg"),
	PYTHON("python.png"),

	QUICK_RESULT("quick_calculation.png"),

	REDO("redo.png"),
	REDO_DISABLED("redo_disabled.png"),
	REGIONALIZED_RESULT("model/impact_method.png"),
	REMOVE_SUPPLY_CHAIN("graphical/remove_supply_chain.png"),
	REFRESH("refresh.png"),
	RESET_ALL_CHANGES("cloud/reset_all.png"),
	RESET_SELECTED_CHANGE("cloud/reset_selected.png"),
	RUN("run.png"),

	SANKEY_OPTIONS("graphical/sankey_options.png"),
	SAVE("save.png"),
	SAVE_DISABLED("save_disabled.png"),
	SAVE_ALL("save_all.png"),
	SAVE_ALL_DISABLED("save_all_disabled.png"),
	SAVE_AS("save_as.png"),
	SAVE_AS_DISABLED("save_as_disabled.png"),
	SAVE_AS_IMAGE("save_as_image.png"),
	SEARCH("search.png"),
	SIMAPRO_WIZARD("wizard/simapro.png"),
	SIMULATE("calculation/simulate.png"),
	SHOW_COMMENTS("cloud/show_comments.png"),
	SQL("sql.png"),

	UNDO("undo.png"),
	UNDO_DISABLED("undo_disabled.png"),
	UP("up.png"),
	UP_DISABLED("up_disabled.png"),
	UP_DOUBLE("up_double.png"),
	UP_DOUBLE_DISABLED("up_double_disabled.png"),

	VALIDATE("validate.png"),

	WARNING("warning.png");

	final String fileName;

	Icon(String fileName) {
		this.fileName = fileName;
	}

	public Image get() {
		return ImageManager.get(this);
	}

	public ImageDescriptor descriptor() {
		return ImageManager.descriptor(this);
	}

	public static Image get(MaterialIcon icon) {
		var image = ImageManager.registry.get(icon.name());
		if (image != null && !image.isDisposed())
			return image;
		image = new Image(Display.getDefault(), icon.data());
		ImageManager.registry.put(icon.name(), image);
		return image;
	}

	public static Image get(MaterialIcon icon, RGB rgb) {
		if (rgb == null)
			return get(icon);
		var key = icon.name() + rgb;
		var image = ImageManager.registry.get(key);
		if (image != null && !image.isDisposed())
			return image;
		image = new Image(Display.getDefault(), icon.data(rgb));
		ImageManager.registry.put(key, image);
		return image;
	}

	public static ImageDescriptor descriptor(MaterialIcon icon) {
		var img = get(icon);
		return new ImageDescriptor() {
			@Override
			public ImageData getImageData(int zoom) {
				return img.getImageData();
			}
		};
	}

	public static ImageDescriptor descriptor(MaterialIcon icon, RGB rgb) {
		var img = get(icon, rgb);
		return new ImageDescriptor() {
			@Override
			public ImageData getImageData(int zoom) {
				return img.getImageData();
			}
		};
	}
}
