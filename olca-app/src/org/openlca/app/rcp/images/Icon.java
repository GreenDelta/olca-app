package org.openlca.app.rcp.images;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public enum Icon {

	ACCEPT("graphical/accept.png"),
	ADD("add.png"),
	ADD_DISABLED("add_disabled.png"),
	ANALYSIS_RESULT("analysis.png"),

	BUILD_SUPPLY_CHAIN("graphical/build_supply_chain.png"),
	BUILDING("building.png"),

	CALCULATE_COSTS("calculation/calculate_costs.png"),
	CALCULATION_WIZARD("wizard/calculation.gif"),
	CHANGE("change.png"),
	CHART("chart.png"),
	CHECK_FALSE("check_false.png"),
	CHECK_TRUE("check_true.png"),
	CLONE("collaboration/clone.png"),
	COLLAPSE("collapse.png"),
	COMMENT("collaboration/comment.png"),
	COMMENTS_VIEW("collaboration/comments_view.png"),
	COMMIT("collaboration/commit.png"),
	COMPARE_COMMIT("collaboration/compare_commit.png"),
	COMPARE_VIEW("collaboration/compare_view.png"),
	CONNECT("connect.png"),
	CONSOLE("console.png"),
	COPY("copy.png"),
	COPY_ALL_CHANGES("collaboration/copy_all.png"),
	COPY_SELECTED_CHANGE("collaboration/copy_selected.png"),
	COLLABORATION_SERVER_LOGO("collaboration/logo.png"),
	CUT("cut.png"),

	DATABASE("model/database.png"),
	DATABASE_DISABLED("model/database_disabled.png"),
	DATABASE_IMPORT("database_import.png"),
	DATABASE_EXPORT("database_export.png"),
	DATABASE_WIZARD("wizard/database.png"),
	DELETE("delete.png"),
	DELETE_DISABLED("delete_disabled.png"),
	DISCONNECT("disconnect.png"),
	DISCONNECT_REPOSITORY("collaboration/disconnect.png"),
	DOWN("down.png"),
	DOWN_DISABLED("down_disabled.png"),

	EC3_WIZARD("wizard/ec3.png"),
	EDIT("edit.png"),
	ERROR("error.png"),
	EXCHANGE_BG_LEFT("graphical/exchange_bg_left.jpg"),
	EXCHANGE_BG_MIDDLE("graphical/exchange_bg_middle.jpg"),
	EXCHANGE_BG_RIGHT("graphical/exchange_bg_right.jpg"),
	EXPAND("expand.png"),
	EXPORT("export.png"),
	EXPORT_WIZARD("wizard/export.png"),
	EXPRESSION("expression.png"),
	EXTENSION("extension.gif"),

	FETCH("collaboration/fetch.png"),
	FILE("file.png"),
	FIREFOX("firefox.png"),
	FOLDER("folder.png"),
	FOLDER_BLUE("folder_blue.png"),
	FOLDER_OPEN("folder_open.png"),
	FORMULA("formula.png"),

	GRAPH_PROCESS_PRODUCTION("graphical/process_production.png"),
	GREEN_DOT("green_dot.png"),
	GIT_GRAPH_FIRST_LAST("collaboration/graph/first-last.png"),
	GIT_GRAPH_FIRST_LOCAL("collaboration/graph/first-local.png"),
	GIT_GRAPH_FIRST_BRANCH_START("collaboration/graph/first-branch-start.png"),
	GIT_GRAPH_LAST_LOCAL("collaboration/graph/last-local.png"),
	GIT_GRAPH_LAST_REMOTE("collaboration/graph/last-remote.png"),
	GIT_GRAPH_LAST_BRANCH_END("collaboration/graph/last-branch-end.png"),
	GIT_GRAPH_LOCAL("collaboration/graph/local.png"),
	GIT_GRAPH_BRANCH_START("collaboration/graph/branch-start.png"),
	GIT_GRAPH_BRANCH_END("collaboration/graph/branch-end.png"),
	GIT_GRAPH_BRANCH_END_BRANCH_START("collaboration/graph/branch-end-branch-start.png"),
	GIT_GRAPH_BRANCH_LOCAL("collaboration/graph/branch-local.png"),
	GIT_GRAPH_BRANCH_LOCAL_END("collaboration/graph/branch-local-end.png"),
	GIT_GRAPH_BRANCH_REMOTE("collaboration/graph/branch-remote.png"),
	GIT_GRAPH_BRANCH_REMOTE_END	("collaboration/graph/branch-remote-end.png"),

	HELP("help.png"),
	HISTORY_VIEW("collaboration/history_view.png"),
	HOME("home.png"),

	IMPORT("import.png"),
	IMPORT_WIZARD("wizard/import.png"),
	IMPORT_ZIP_WIZARD("wizard/zip.png"),
	INFO("info.png"),
	INPUT("model/input.png"),
	IPC("ipc.png"),

	LAYOUT("graphical/layout.png"),
	LIBRARY("library.png"),
	LINK("link.png"),
	LOCK("lock.png"),
	LOGO("plugin/logo_32_32bit.png"),

	MANAGE_PLUGINS("manage_plugins.png"),
	MAP("map.png"),
	MAXIMIZE("graphical/maximize.png"),
	MERGE("collaboration/merge.png"),
	MINIMAP("graphical/minimap.png"),
	MINIMIZE("graphical/minimize.png"),
	MINUS("graphical/minus.gif"),

	NEXT_CHANGE("collaboration/next_change.png"),
	NEW_WIZARD("wizard/new.png"),
	NUMBER("number.png"),

	OPEN_FOLDER("open_folder.png"),
	OUTLINE("graphical/outline.png"),
	OUTPUT("model/output.png"),

	PASTE("paste.png"),
	PLUS("graphical/plus.gif"),
	PREFERENCES("preferences.png"),
	PREVIOUS_CHANGE("collaboration/previous_change.png"),
	PROCESS_BG("graphical/process_bg.jpg"),
	PROCESS_ADD("graphical/process_add.png"),
	PROCESS_BG_LCI("graphical/process_bg_lci.jpg"),
	PROCESS_BG_SYS("graphical/process_bg_sys.jpg"),
	PROCESS_BG_MARKED("graphical/process_bg_marked.jpg"),
	PROCESS_BG_LIB("graphical/process_bg_lib.jpg"),
	PULL("collaboration/pull.png"),
	PUSH("collaboration/push.png"),
	PYTHON("python.png"),

	QUICK_RESULT("quick_calculation.png"),

	RED_DOT("red_dot.png"),
	REDO("redo.png"),
	REDO_DISABLED("redo_disabled.png"),
	REGIONALIZED_RESULT("model/impact_method.png"),
	REMOVE_SUPPLY_CHAIN("graphical/remove_supply_chain.png"),
	REFRESH("refresh.png"),
	REPOSITORY("collaboration/repository.png"),
	RESET_ALL_CHANGES("collaboration/reset_all.png"),
	RESET_SELECTED_CHANGE("collaboration/reset_selected.png"),
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
	SQL("sql.png"),
	STASH("collaboration/stash.png"),
	STASH_CREATE("collaboration/stash_create.png"),
	STASH_APPLY("collaboration/stash_apply.png"),

	TARGET("graphical/target.png"),

	UNDO("undo.png"),
	UNDO_DISABLED("undo_disabled.png"),
	UP("up.png"),
	UPDATE("update.png"),
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

}
