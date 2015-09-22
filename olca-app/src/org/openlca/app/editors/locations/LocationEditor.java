package org.openlca.app.editors.locations;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.core.model.Location;
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
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		String kml = infoPage.getKml();
		if (kml == null)
			getModel().setKmz(null);
		else
			getModel().setKmz(KmlUtil.toKmz(kml));
		super.doSave(monitor);
	}

}
