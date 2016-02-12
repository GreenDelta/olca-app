package org.openlca.app.editors.processes.kml;

public interface EditorHandler {

	boolean contentSaved(String kml);

	void openModel();
	
	boolean hasModel();

}
