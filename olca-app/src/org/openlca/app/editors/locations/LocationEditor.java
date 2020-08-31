package org.openlca.app.editors.locations;

import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocationEditor extends ModelEditor<Location> {

	public static String ID = "editors.location";
	private final Logger log = LoggerFactory.getLogger(getClass());

	public LocationEditor() {
		super(Location.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new LocationInfoPage(this));
			addCommentPage();
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
