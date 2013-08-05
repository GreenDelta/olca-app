package org.openlca.app.plugin;

import java.io.File;
import java.io.IOException;

import org.openlca.app.html.HtmlFolder;
import org.openlca.app.html.HtmlResource;
import org.openlca.app.html.IHtmlResource;
import org.slf4j.LoggerFactory;

enum Resource {

	USAGE_VIEW_JS("js" + File.separator + "usage_view.js");

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
			LoggerFactory.getLogger(getClass()).error(
					"Error loading html resource", e);
		}
		return resource;
	}

}
