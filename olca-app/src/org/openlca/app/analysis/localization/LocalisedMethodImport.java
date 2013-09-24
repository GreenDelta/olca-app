package org.openlca.app.analysis.localization;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.jobs.Status;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.io.xls.Excel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Imports a localised impact assessment method from an Excel file.
 */
public class LocalisedMethodImport implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());
	private File file;
	private IDatabase database;
	private EntityCache cache;
	private Status status = new Status(Status.WAITING);
	private LocalisedImpactMethod method;
	private List<Location> locations;

	public LocalisedMethodImport(File file, IDatabase database,
			EntityCache entityCache) {
		this.file = file;
		this.database = database;
		this.cache = entityCache;
	}

	public Status getStatus() {
		return status;
	}

	public LocalisedImpactMethod getMethod() {
		return method;
	}

	@Override
	public void run() {
		status = new Status(Status.RUNNING);
		try {
			locations = database.createDao(Location.class).getAll();
			try (FileInputStream in = new FileInputStream(file)) {
				HSSFWorkbook workbook = new HSSFWorkbook(in);
				HSSFSheet infoSheet = workbook.getSheet("method_info");
				createMethod(infoSheet);
				importCategories(workbook);
			}
			status = new Status(Status.OK);
		} catch (Exception e) {
			log.error("Failed to import impact method", e);
			status = new Status(Status.FAILED);
		}
	}

	private void createMethod(HSSFSheet infoSheet) {
		String methodId = Excel.getString(infoSheet, 3, 3);
		log.trace("Import method {}", methodId);
		ImpactMethodDao dao = new ImpactMethodDao(database);
		ImpactMethod realMethod = dao.getForRefId(methodId);
		if (realMethod == null)
			throw new RuntimeException("Unkown impact method " + methodId);
		method = new LocalisedImpactMethod();
		method.setImpactMethod(Descriptors.toDescriptor(realMethod));
	}

	private void importCategories(HSSFWorkbook workbook) {
		int sheetCount = workbook.getNumberOfSheets();
		for (int i = 0; i < sheetCount; i++) {
			HSSFSheet sheet = workbook.getSheetAt(i);
			if (isCategorySheet(sheet))
				importCategorySheet(sheet);
		}
	}

	private boolean isCategorySheet(HSSFSheet sheet) {
		if (sheet == null)
			return false;
		String label = Excel.getString(sheet, 1, 1);
		if (label == null)
			return false;
		return "Impact category".equals(label);
	}

	private void importCategorySheet(HSSFSheet sheet) {
		String id = Excel.getString(sheet, 2, 2);
		log.trace("Import impact category {}", id);
		LocalisedImpactCategory category = new LocalisedImpactCategory();
		ImpactCategoryDao dao = new ImpactCategoryDao(database);
		ImpactCategory realCategory = dao.getForRefId(id);
		if (realCategory == null)
			throw new RuntimeException("Unknown impact category " + id);
		category.setImpactCategory(Descriptors.toDescriptor(realCategory));
		method.getImpactCategories().add(category);
		List<Location> usedLocations = getLocations(sheet);
		if (usedLocations.isEmpty())
			return;
		List<LocalisedImpactFactor> factors = readFactors(sheet, usedLocations);
		category.getFactors().addAll(factors);
	}

	private List<Location> getLocations(HSSFSheet sheet) {
		List<Location> usedLocations = new ArrayList<>();
		String nextCode = null;
		int nextCol = 6;
		while ((nextCode = Excel.getString(sheet, 4, nextCol)) != null) {
			log.trace("with location {}", nextCode);
			Location location = findLocation(nextCode);
			if (location == null)
				break;
			usedLocations.add(location);
			nextCol++;
		}
		return usedLocations;
	}

	private Location findLocation(String code) {
		for (Location location : locations) {
			if (!code.equalsIgnoreCase(location.getCode()))
				continue;
			if (!method.getLocations().contains(location))
				method.getLocations().add(location);
			return unwrapLocation(location);
		}
		log.warn("unknown location {}", code);
		return null;
	}

	private Location unwrapLocation(Location location) {
		if (location == null)
			return null;
		Location unwrapped = new Location();
		unwrapped.setName(location.getName());
		unwrapped.setCode(location.getCode());
		unwrapped.setLatitude(location.getLatitude());
		unwrapped.setLongitude(location.getLongitude());
		unwrapped.setDescription(location.getDescription());
		unwrapped.setId(location.getId());
		return unwrapped;
	}

	private List<LocalisedImpactFactor> readFactors(HSSFSheet sheet,
			List<Location> usedLocations) {
		List<LocalisedImpactFactor> factors = new ArrayList<>();
		int nextRow = 5;
		FlowDao dao = new FlowDao(database);
		String flowId = null;
		while ((flowId = Excel.getString(sheet, nextRow, 1)) != null) {
			Flow flow = dao.getForRefId(flowId);
			if (flow == null)
				continue;
			LocalisedImpactFactor factor = new LocalisedImpactFactor();
			factor.setFlow(Descriptors.toDescriptor(flow));
			factors.add(factor);
			for (int i = 0; i < usedLocations.size(); i++) {
				Location location = usedLocations.get(i);
				int col = i + 6;
				double val = Excel.getDouble(sheet, nextRow, col);
				factor.addValue(location, val);
			}
			nextRow++;
		}
		return factors;
	}
}
