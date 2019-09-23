package org.openlca.app.rcp.html;

import org.openlca.app.rcp.RcpActivator;

public enum HtmlView {

	// KML_EDITOR("kml_editor.html"),

	KML_RESULT_VIEW("kml_result_view.html");

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