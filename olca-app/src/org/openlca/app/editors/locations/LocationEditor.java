package org.openlca.app.editors.locations;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.Location;

public class LocationEditor extends ModelEditor<Location> {

	public static String ID = "editors.location";

	public LocationEditor() {
		super(Location.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new LocationInfoPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("failed to add page", e);
		}
	}

}
