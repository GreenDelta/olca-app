package org.openlca.app.editors.locations;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.Location;
import org.openlca.util.Geometries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocationEditor extends ModelEditor<Location> {

	public static String ID = "editors.location";
	private Logger log = LoggerFactory.getLogger(getClass());
	private LocationInfoPage infoPage;

	public LocationEditor() {
		super(Location.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(infoPage = new LocationInfoPage(this));
			addPage(new MapPage(this));
			addCommentPage();
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		Location loc = getModel();
		if (loc.kmz == null) {
			double lat = getModel().latitude;
			double lon = getModel().longitude;
			if (lat != 0 || lon != 0) {
				String kml = Geometries.pointToKml(lat, lon);
				getModel().kmz = Geometries.kmlToKmz(kml);
			}
			infoPage.refreshKmlView();
		}
		super.doSave(monitor);
	}
}
