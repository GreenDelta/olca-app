package org.openlca.app.rcp.html;

import org.openlca.app.rcp.RcpActivator;

public enum HtmlView {

	BUBBLE_CHART("bubble_chart.html"),

	GMAP_HEATMAP("gmap_heatmap.html"),

	GRAPH_VIEW("graph_view.html"),

	JAVASCRIPT_EDITOR("javascript_editor.html"),

	KML_EDITOR("kml_editor.html"),

	KML_RESULT_VIEW("kml_result_view.html"),

	PRODUCT_SYSTEM_STATISTICS("product_system_statistics.html"),

	PYTHON_EDITOR("python_editor.html"),

	REPORT_VIEW("report_view.html"),

	START_PAGE("start_page.html"),

	SUNBURST_CHART("sunburst_chart.html"),

	TREEMAP("treemap.html"),

	USAGES_VIEW("usages_view.html");

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