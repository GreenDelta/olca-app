package org.openlca.app.editors.locations;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.lcia_methods.shapefiles.ShapeFileUtils;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.geo.parameter.ParameterCache;
import org.openlca.geo.parameter.ShapeFileFolder;
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
			// TODO: refresh KML view in infoPage
		}
		invalidateIntersections();
		super.doSave(monitor);
	}

	private void invalidateIntersections() {
		ImpactMethodDao dao = new ImpactMethodDao(Database.get());
		for (ImpactMethodDescriptor method : dao.getDescriptors()) {
			ShapeFileFolder folder = new ShapeFileFolder(
					ShapeFileUtils.getFolder(method));
			ParameterCache cache = new ParameterCache(folder);
			for (String shapeFile : ShapeFileUtils.getShapeFiles(method))
				cache.remove(getModel().id, shapeFile);
		}
	}

}
