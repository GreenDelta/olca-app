package org.openlca.app.html;

import static org.openlca.app.html.Resource.USAGE_VIEW_JS;

import java.io.File;
import java.io.IOException;

import org.openlca.app.rcp.RcpActivator;
import org.slf4j.LoggerFactory;

public enum HtmlView {

	BUBBLE_CHART("bubble_chart.html"),

	GMAP_HEATMAP("gmap_heatmap.html"),

	GRAPH_VIEW("graph_view.html"),

	IMPACT_LOCALISATION_PAGE("impact_localisation_page.html"),

	KML_EDITOR("kml_editor.html"),

	KML_RESULT_VIEW("kml_result_view.html"),

	PRODUCT_SYSTEM_STATISTICS("product_system_statistics.html"),

	REPORT_VIEW("report_view.html"),

	RESULT_LOCALISED_LCIA("result_localised_lcia.html", Resource.OLCA_CHARTS_JS),

	START_PAGE("start_page.html"),

	SUNBURST_CHART("sunburst_chart.html"),

	TREEMAP("treemap.html"),

	USAGES_VIEW("usages_view.html", USAGE_VIEW_JS);

	private IHtmlResource resource;
	private Resource[] dependencies;

	private HtmlView(String fileName, Resource... dependencies) {
		this.resource = new HtmlResource(RcpActivator.getDefault().getBundle(),
				"html" + File.separator + fileName, fileName);
		this.dependencies = dependencies;
	}

	public IHtmlResource getResource() {
		try {
			if (dependencies != null)
				for (Resource dependency : dependencies)
					HtmlFolder.register(dependency.getResource());
			HtmlFolder.register(resource);
		} catch (IOException e) {
			LoggerFactory.getLogger(getClass()).error(
					"Error registering html resource", e);
		}
		return resource;
	}

}