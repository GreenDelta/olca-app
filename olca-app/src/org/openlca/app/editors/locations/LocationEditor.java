package org.openlca.app.editors.locations;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.lcia_methods.shapefiles.ShapeFileUtils;
import org.openlca.app.util.Info;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.geo.parameter.ParameterCache;
import org.openlca.geo.parameter.ShapeFileFolder;
import org.openlca.util.Geometries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

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
		if (!infoPage.hasValidKml) {
			Info.showBox("Kml editor",
					"The provided kml is invalid, please check your input");
			return;
		}
		String kml = infoPage.kml;
		if (!Strings.isNullOrEmpty(kml)) {
			getModel().kmz = Geometries.kmlToKmz(kml);
		} else {
			double latitude = getModel().latitude;
			double longitude = getModel().longitude;
			if (latitude != 0 || longitude != 0) {
				kml = Geometries.pointToKml(latitude, longitude);
				getModel().kmz = Geometries.kmlToKmz(kml);
				infoPage.updateKml();
			} else {
				getModel().kmz = null;
			}
		}
		invalidateIntersections();
		super.doSave(monitor);
	}

	private void invalidateIntersections() {
		ImpactMethodDao methodDao = new ImpactMethodDao(Database.get());
		List<ImpactMethodDescriptor> descriptors = methodDao.getDescriptors();
		for (ImpactMethodDescriptor method : descriptors) {
			ShapeFileFolder folder = new ShapeFileFolder(
					ShapeFileUtils.getFolder(method));
			ParameterCache cache = new ParameterCache(folder);
			for (String shapeFile : ShapeFileUtils.getShapeFiles(method))
				cache.remove(getModel().id, shapeFile);
		}
	}

}
