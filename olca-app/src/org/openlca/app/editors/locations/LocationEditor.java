package org.openlca.app.editors.locations;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.lcia.shapefiles.ShapeFileUtils;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
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
		invalidateIntersections();
		super.doSave(monitor);
	}

	private void invalidateIntersections() {
		ImpactCategoryDao dao = new ImpactCategoryDao(Database.get());
		for (ImpactCategoryDescriptor d : dao.getDescriptors()) {
			ShapeFileFolder folder = new ShapeFileFolder(
					ShapeFileUtils.getFolder(d));
			ParameterCache cache = new ParameterCache(folder);
			for (String shapeFile : ShapeFileUtils.getShapeFiles(d)) {
				cache.remove(getModel().id, shapeFile);
			}
		}
	}

}
