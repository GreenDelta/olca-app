package org.openlca.core.editors;

import java.io.File;
import java.io.IOException;

import org.openlca.app.html.HtmlFolder;
import org.openlca.app.html.HtmlResource;
import org.openlca.app.html.IHtmlResource;
import org.openlca.core.application.plugin.Activator;

enum Resource {

	OLCA_CHARTS_JS("js" + File.separator + "olca.charts.js");

	private IHtmlResource resource;
	private Resource[] dependencies;

	private Resource(String fileName, Resource... dependencies) {
		this.resource = new HtmlResource(Activator.getDefault().getBundle(),
				"html" + File.separator + fileName, fileName);
		this.dependencies = dependencies;
	}

	IHtmlResource getResource() {
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
