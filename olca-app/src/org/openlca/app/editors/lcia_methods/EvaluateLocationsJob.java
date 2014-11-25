package org.openlca.app.editors.lcia_methods;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.geo.kml.KmlFeature;
import org.openlca.geo.parameter.ParameterRepository;
import org.openlca.geo.parameter.ParameterSetBuilder;
import org.openlca.geo.parameter.ShapeFileRepository;
import org.python.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluateLocationsJob implements IRunnableWithProgress {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ImpactMethod method;
	private ShapeFileRepository shapeFileRepository;
	private ParameterRepository parameterRepository;
	private ParameterSetBuilder setBuilder;
	private LocationDao locationDao;
	private IProgressMonitor monitor;
	private boolean forceEvaluation;
	private List<String> shapeFiles;

	public EvaluateLocationsJob(ImpactMethod method) {
		this.method = method;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		this.monitor = monitor;
		List<BaseDescriptor> locations = init();
		beginTask("Evaluating locations", locations.size());
		for (BaseDescriptor location : locations)
			if (!isCanceled())
				evaluate(location);
		done();
	}

	private List<BaseDescriptor> init() {
		beginTask("Initializing");
		shapeFileRepository = new ShapeFileRepository(
				ShapeFileUtils.getFolder(method));
		shapeFiles = shapeFileRepository.getShapeFiles();
		if (shapeFiles.size() == 0)
			return Collections.emptyList();
		parameterRepository = new ParameterRepository(shapeFileRepository);
		List<Parameter> parameters = getShapeFileParameters();
		if (parameters.size() == 0)
			return Collections.emptyList();
		setBuilder = ParameterSetBuilder.createBuilder(parameters,
				shapeFileRepository, parameterRepository);
		locationDao = new LocationDao(Database.get());
		return locationDao.getDescriptors();
	}

	private void evaluate(BaseDescriptor location) {
		subTask(location.getName());
		if (needsEvaluation(location)) {
			KmlFeature feature = getKmlFeature(location);
			if (feature != null)
				setBuilder.build(Collections.singleton(feature));
		}
		worked();
	}

	private boolean needsEvaluation(BaseDescriptor location) {
		if (forceEvaluation)
			return true;
		KmlFeature feature = KmlFeature.empty();
		feature.setIdentifier(location.getRefId());
		for (String shapeFile : shapeFiles)
			if (!parameterRepository.contains(feature, shapeFile))
				return true;
		return false;
	}

	private List<Parameter> getShapeFileParameters() {
		if (method == null)
			return Collections.emptyList();
		long methodId = method.getId();
		String query = "select m.parameters from ImpactMethod m where "
				+ "m.id = :methodId";
		ParameterDao dao = new ParameterDao(Database.get());
		List<Parameter> allParams = dao.getAll(query,
				Collections.singletonMap("methodId", methodId));
		List<Parameter> shapeFileParams = new ArrayList<>();
		for (Parameter param : allParams) {
			if (param.getExternalSource() == null)
				continue;
			if (!"SHAPE_FILE".equals(param.getSourceType()))
				continue;
			shapeFileParams.add(param);
		}
		return shapeFileParams;
	}

	private KmlFeature getKmlFeature(BaseDescriptor location) {
		byte[] kmz = locationDao.getKmz(location.getId());
		if (kmz == null)
			return null;
		String kml = KmlUtil.toKml(kmz);
		if (Strings.isNullOrEmpty(kml))
			return null;
		try {
			KmlFeature feature = KmlFeature.parse(kml);
			feature.setIdentifier(location.getRefId());
			return feature;
		} catch (Exception e) {
			log.warn("Could not parse kml data for location "
					+ location.getName());
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
