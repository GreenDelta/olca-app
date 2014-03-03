package org.openlca.app.html;

import static org.openlca.app.html.Resource.USAGE_VIEW_JS;

import java.io.File;
import java.io.IOException;

import org.openlca.app.rcp.RcpActivator;
import org.slf4j.LoggerFactory;

public enum HtmlView {

	USAGES_VIEW("usages_view.html", USAGE_VIEW_JS),

	GMAP_HEATMAP("gmap_heatmap.html"),

	IMPACT_LOCALISATION_PAGE("impact_localisation_page.html"),

	RESULT_LOCALISED_LCIA("result_localised_lcia.html", Resource.OLCA_CHARTS_JS),

	SUNBURST_CHART("sunburst_chart.html"),

	BUBBLE_CHART("bubble_chart.html"),

	TREEMAP("treemap.html");

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