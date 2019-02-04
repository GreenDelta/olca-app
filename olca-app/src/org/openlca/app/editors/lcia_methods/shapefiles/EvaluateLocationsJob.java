package org.openlca.app.editors.lcia_methods.shapefiles;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ImpactMethod.ParameterMean;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.descriptors.LocationDescriptor;
import org.openlca.geo.kml.KmlFeature;
import org.openlca.geo.parameter.ParameterCache;
import org.openlca.geo.parameter.ParameterCalculator;
import org.openlca.geo.parameter.ShapeFileFolder;
import org.python.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EvaluateLocationsJob implements IRunnableWithProgress {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ImpactMethod method;
	private ShapeFileFolder shapeFileFolder;
	private ParameterCache parameterCache;
	private ParameterCalculator parameterCalculator;
	private LocationDao locationDao;
	private IProgressMonitor monitor;
	private List<String> shapeFiles;

	public EvaluateLocationsJob(ImpactMethod method) {
		this.method = method;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		this.monitor = monitor;
		List<LocationDescriptor> locations = init();
		beginTask(M.EvaluatingLocations, locations.size());
		for (LocationDescriptor location : locations)
			if (!isCanceled())
				evaluate(location);
		done();
	}

	private List<LocationDescriptor> init() {
		beginTask(M.Initializing);
		shapeFileFolder = new ShapeFileFolder(ShapeFileUtils.getFolder(method));
		shapeFiles = shapeFileFolder.getShapeFiles();
		if (shapeFiles.size() == 0)
			return Collections.emptyList();
		parameterCache = new ParameterCache(shapeFileFolder);
		List<Parameter> parameters = getShapeFileParameters();
		if (parameters.size() == 0)
			return Collections.emptyList();
		ParameterMean meanFn = method.parameterMean != null
				? method.parameterMean
				: ParameterMean.WEIGHTED_MEAN;
		parameterCalculator = new ParameterCalculator(parameters,
				shapeFileFolder, meanFn);
		locationDao = new LocationDao(Database.get());
		return locationDao.getDescriptors();
	}

	private void evaluate(LocationDescriptor location) {
		subTask(location.name);
		for (String shapeFile : ShapeFileUtils.getShapeFiles(method))
			parameterCache.remove(location.id, shapeFile);
		KmlFeature feature = getKmlFeature(location);
		if (feature != null)
			parameterCalculator.calculate(location.id, feature);
		worked();
	}

	private List<Parameter> getShapeFileParameters() {
		if (method == null)
			return Collections.emptyList();
		long methodId = method.id;
		String query = "select m.parameters from ImpactMethod m where " + "m.id = :methodId";
		ParameterDao dao = new ParameterDao(Database.get());
		List<Parameter> allParams = dao.getAll(query, Collections.singletonMap("methodId", methodId));
		List<Parameter> shapeFileParams = new ArrayList<>();
		for (Parameter param : allParams) {
			if (param == null)
				continue;
			if (param.externalSource == null)
				continue;
			if (!"SHAPE_FILE".equals(param.sourceType))
				continue;
			shapeFileParams.add(param);
		}
		return shapeFileParams;
	}

	private KmlFeature getKmlFeature(LocationDescriptor location) {
		byte[] kmz = locationDao.getForId(location.id).kmz;
		if (kmz == null)
			return null;
		String kml = KmlUtil.toKml(kmz);
		if (Strings.isNullOrEmpty(kml))
			return null;
		try {
			KmlFeature feature = KmlFeature.parse(kml);
			return feature;
		} catch (Exception e) {
			log.warn("Could not parse kml data for location " + location.name);
		}
		return null;
	}

	private void beginTask(String name) {
		beginTask(name, IProgressMonitor.UNKNOWN);
	}

	private void beginTask(String name, int totalWork) {
		if (totalWork == 0)
			return;
		if (monitor != null)
			monitor.beginTask(name, totalWork);
	}

	private void worked() {
		if (monitor != null)
			monitor.worked(1);
	}

	private void subTask(String name) {
		if (monitor != null)
			monitor.subTask(name);
	}

	private void done() {
		if (monitor != null)
			monitor.done();
	}

	private boolean isCanceled() {
		if (monitor == null)
			return false;
		return monitor.isCanceled();
	}

}
