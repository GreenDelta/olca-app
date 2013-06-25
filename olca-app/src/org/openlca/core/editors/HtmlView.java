package org.openlca.core.editors;

import static org.openlca.core.editors.Resource.OLCA_CHARTS_JS;

import java.io.File;
import java.io.IOException;

import org.openlca.core.application.plugin.Activator;
import org.openlca.ui.html.HtmlFolder;
import org.openlca.ui.html.HtmlResource;
import org.openlca.ui.html.IHtmlResource;

public enum HtmlView {

	GMAP_HEATMAP("gmap_heatmap.html"),

	IMPACT_LOCALISATION_PAGE("impact_localisation_page.html"),

	RESULT_LOCALISED_LCIA("result_localised_lcia.html", OLCA_CHARTS_JS),

	SUNBURST_CHART("sunburst_chart.html");

	private IHtmlResource resource;
	private Resource[] dependencies;

	private HtmlView(String fileName, Resource... dependencies) {
		this.resource = new HtmlResource(Activator.getDefault().getBundle(),
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
			e.printStackTrace();
		}
		return resource;
	}

}
