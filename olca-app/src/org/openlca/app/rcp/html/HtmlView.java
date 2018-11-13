package org.openlca.app.rcp.html;

import org.openlca.app.rcp.RcpActivator;

public enum HtmlView {

	GMAP_HEATMAP("location_heatmap.html"),

	JAVASCRIPT_EDITOR("javascript_editor.html"),

	KML_EDITOR("kml_editor.html"),

	KML_RESULT_VIEW("kml_result_view.html"),

	PRODUCT_SYSTEM_STATISTICS("product_system_statistics.html"),

	PYTHON_EDITOR("python_editor.html"),

	REPORT_VIEW("report_view.html"),

	START_PAGE("start_page.html"),

	COMMENTS("comments.html");

	private final String fileName;

	private HtmlView(String fileName) {
		this.fileName = fileName;
	}

	public String getUrl() {
		return HtmlFolder.getUrl(RcpActivator.getDefault().getBundle(),
				fileName);
	}

	public String getFileName() {
		return fileName;
	}

}