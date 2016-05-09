package org.openlca.app.editors.locations;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.lcia_methods.ShapeFileUtils;
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
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (!infoPage.isValidKml()) {
			Info.showBox("Kml editor", "The provided kml is invalid, please check your input");
			return;
		}
		String kml = infoPage.getKml();
		if (Strings.isNullOrEmpty(kml)) {
			double latitude = getModel().getLatitude();
			double longitude = getModel().getLongitude();
			if (latitude != 0 || longitude != 0) {
				kml = Geometries.pointToKml(latitude, longitude);
				getModel().setKmz(Geometries.kmlToKmz(kml));
				infoPage.updateKml();
			} else
				getModel().setKmz(null);
		} else
			getModel().setKmz(Geometries.kmlToKmz(kml));
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
				cache.remove(getModel().getId(), shapeFile);
		}
	}

}
