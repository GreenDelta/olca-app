package org.openlca.app.editors.locations;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.lcia_methods.ShapeFileUtils;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.app.util.Info;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.geo.parameter.ParameterRepository;
import org.openlca.geo.parameter.ShapeFileRepository;
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
		if (!infoPage.isValidKml()) {
			Info.showBox("Kml editor", "The provided kml is invalid, please check your input");
			return;
		}
		String kml = infoPage.getKml();
		if (kml == null)
			getModel().setKmz(null);
		else
			getModel().setKmz(KmlUtil.toKmz(kml));
		invalidateIntersections();
		super.doSave(monitor);
	}

	private void invalidateIntersections() {
		ImpactMethodDao methodDao = new ImpactMethodDao(Database.get());
		List<ImpactMethodDescriptor> descriptors = methodDao.getDescriptors();
		for (ImpactMethodDescriptor method : descriptors) {
			ShapeFileRepository repo = new ShapeFileRepository(
					ShapeFileUtils.getFolder(method));
			ParameterRepository pRepo = new ParameterRepository(repo);
			for (String shapeFile : ShapeFileUtils.getShapeFiles(method))
				pRepo.remove(getModel().getId(), shapeFile);
		}
	}

}
