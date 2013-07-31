package org.openlca.core.editors.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openlca.app.App;
import org.openlca.app.util.Labels;
import org.openlca.core.database.IDatabase;
import org.openlca.core.editors.model.FlowInfo;
import org.openlca.core.editors.model.FlowInfoDao;
import org.openlca.core.editors.model.ProductInfo;
import org.openlca.core.editors.model.ProductInfoDao;
import org.openlca.core.math.AllocationMatrix;
import org.openlca.core.math.FlowIndex;
import org.openlca.core.math.IMatrix;
import org.openlca.core.math.ImpactMatrix;
import org.openlca.core.math.ImpactMatrixBuilder;
import org.openlca.core.math.Index;
import org.openlca.core.math.InventoryMatrix;
import org.openlca.core.math.InventoryMatrixBuilder;
import org.openlca.core.math.MatrixFactory;
import org.openlca.core.math.ProductIndex;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemExport {

	private Logger log = LoggerFactory.getLogger(getClass());

	private InventoryMatrix inventoryMatrix;
	private AllocationMatrix allocationMatrix;
	private ImpactMatrix impactMatrix;

	private ProductSystem productSystem;
	private AllocationMethod allocationMethod;
	private BaseDescriptor impactMethod;

	private FlowInfoDao flowInfoDao;
	private ProductInfoDao productInfoDao;

	private String databaseName;

	public SystemExport(ProductSystem productSystem, IDatabase database) {
		this(productSystem, database, null, null);
	}

	public SystemExport(ProductSystem productSystem, IDatabase database,
			AllocationMethod allocationMethod,
			BaseDescriptor impactMethodDescriptor) {
		this.productSystem = productSystem;
		this.databaseName = database.getName();

		// initialize daos
		flowInfoDao = new FlowInfoDao(database);
		productInfoDao = new ProductInfoDao(database);

		// initialize inventory matrix
		inventoryMatrix = new InventoryMatrixBuilder(productSystem).build();
		if (allocationMethod != null
				&& allocationMethod != AllocationMethod.None) {
			// initialize allocation matrix
			this.allocationMethod = allocationMethod;
			allocationMatrix = AllocationMatrix.create(inventoryMatrix,
					productSystem, allocationMethod, database);
		}
		if (impactMethodDescriptor != null) {
			this.impactMethod = impactMethodDescriptor;
			try {
				// initialize impact matrix
				this.impactMatrix = new ImpactMatrixBuilder(database).build(
						impactMethodDescriptor, inventoryMatrix.getFlowIndex());
			} catch (Exception e) {
				log.error("Error creating impact matrix", e);
			}
		}
	}

	public void exportTo(File dir) throws IOException {
		File subDir = new File(dir, productSystem.getName());
		if (!subDir.exists()) {
			subDir.mkdirs();
		}

		Workbook elementaryWorkbook = new XSSFWorkbook();
		createElementaryCoverSheet(elementaryWorkbook, null);
		createElementarySheet(elementaryWorkbook);
		writeToFile(elementaryWorkbook, new File(subDir, FILE_NAMES.ELEMENTARY));

		Workbook productWorkbook = new XSSFWorkbook();
		createProductCoverSheet(productWorkbook, null);
		createProductSheet(productWorkbook);
		writeToFile(productWorkbook, new File(subDir, FILE_NAMES.PRODUCT));

		if (allocationMatrix != null) {
			allocationMatrix.apply(inventoryMatrix);

			elementaryWorkbook = new XSSFWorkbook();
			createElementaryCoverSheet(elementaryWorkbook, allocationMethod);
			createElementarySheet(elementaryWorkbook);
			writeToFile(elementaryWorkbook, new File(subDir,
					FILE_NAMES.ELEMENTARY_ALLOCATED));

			productWorkbook = new XSSFWorkbook();
			createProductCoverSheet(productWorkbook, allocationMethod);
			createProductSheet(productWorkbook);
			writeToFile(productWorkbook, new File(subDir,
					FILE_NAMES.PRODUCT_ALLOCATED));
		}

		if (impactMethod != null) {
			Workbook impactMethodWorkbook = new XSSFWorkbook();
			createImpactMethodCoverSheet(impactMethodWorkbook);
			createImpactMethodSheet(impactMethodWorkbook);
			writeToFile(impactMethodWorkbook, new File(subDir,
					FILE_NAMES.IMPACT_FACTORS));
		}
	}

	private void createElementaryCoverSheet(Workbook workbook,
			AllocationMethod allocationMethod) {
		Sheet sheet = workbook.createSheet("General information");

		boolean allocated = allocationMethod != null;
		String subTitle = allocated ? TITLES.ELEMENTARY_ALLOCATED
				: TITLES.ELEMENTARY;

		int currentRow = 0;
		currentRow = writeHeaderInformation(sheet, currentRow, subTitle);
		currentRow++;
		currentRow = writeSoftwareInformation(sheet, currentRow);
		currentRow++;

		String name = productSystem.getName();
		int processes = productSystem.getProcesses().length;
		int products = inventoryMatrix.getProductIndex().size();
		int flows = inventoryMatrix.getFlowIndex().size();
		String dimensions = flows + "x" + products;

		currentRow = line(sheet, currentRow, "Product system:", name);
		if (allocated)
			currentRow = line(sheet, currentRow, "Allocation method:",
					Labels.allocationMethod(allocationMethod));
		currentRow = line(sheet, currentRow, "No. of processes:", processes);
		currentRow = line(sheet, currentRow, "No. of products:", products);
		currentRow = line(sheet, currentRow, "No. of elementary flows:", flows);
		currentRow = line(sheet, currentRow, "Matrix dimensions:", dimensions);

		Excel.autoSize(sheet, new int[] { 0, 1 });
	}

	private void createProductCoverSheet(Workbook workbook,
			AllocationMethod allocationMethod) {
		Sheet sheet = workbook.createSheet("General information");

		boolean allocated = allocationMethod != null;
		String subTitle = allocated ? TITLES.PRODUCT_ALLOCATED : TITLES.PRODUCT;

		int currentRow = 0;
		currentRow = writeHeaderInformation(sheet, currentRow, subTitle);
		currentRow++;
		currentRow = writeSoftwareInformation(sheet, currentRow);
		currentRow++;

		String name = productSystem.getName();
		int processes = productSystem.getProcesses().size();
		int products = inventoryMatrix.getProductIndex().size();
		String dimensions = products + "x" + products;

		currentRow = line(sheet, currentRow, "Product system:", name);
		if (allocated)
			currentRow = line(sheet, currentRow, "Allocation method:",
					Labels.allocationMethod(allocationMethod));
		currentRow = line(sheet, currentRow, "No. of processes:", processes);
		currentRow = line(sheet, currentRow, "No. of products:", products);
		currentRow = line(sheet, currentRow, "Matrix dimensions:", dimensions);

		Excel.autoSize(sheet, new int[] { 0, 1 });
	}

	private void createImpactMethodCoverSheet(Workbook workbook) {
		Sheet sheet = workbook.createSheet("General information");

		int currentRow = 0;
		currentRow = writeHeaderInformation(sheet, currentRow,
				TITLES.IMPACT_FACTORS);
		currentRow++;
		currentRow = writeSoftwareInformation(sheet, currentRow);
		currentRow++;

		String name = productSystem.getName();
		String methodName = impactMethod.getName();
		int categories = impactMatrix.getCategoryIndex().size();
		int factors = impactMatrix.getFlowIndex().size();
		String dimensions = factors + "x" + categories;

		currentRow = line(sheet, currentRow, "Product system:", name);
		currentRow = line(sheet, currentRow, "Impact method:", methodName);
		currentRow = line(sheet, currentRow, "No. of impact categories:",
				categories);
		currentRow = line(sheet, currentRow, "No. of impact factors:", factors);
		currentRow = line(sheet, currentRow, "Matrix dimensions:", dimensions);

		Excel.autoSize(sheet, new int[] { 0, 1 });
	}

	private int writeHeaderInformation(Sheet sheet, int currentRow,
			String subTitle) {
		String date = DateFormat.getDateInstance().format(
				GregorianCalendar.getInstance().getTime());

		Excel.cell(sheet, currentRow, 0, TITLES.MAIN_TITLE);
		currentRow++;
		Excel.cell(sheet, currentRow, 0, subTitle);
		currentRow++;
		currentRow++;
		Excel.cell(sheet, currentRow, 0, date);
		currentRow++;
		return currentRow;
	}

	private int writeSoftwareInformation(Sheet sheet, int currentRow) {
		currentRow = line(sheet, currentRow, "Software:", "openLCA");
		currentRow = line(sheet, currentRow, "Version:", App.getVersion());
		currentRow = line(sheet, currentRow, "Database:", databaseName);
		return currentRow;
	}

	private int line(Sheet sheet, int row, String designator, String value) {
		Excel.cell(sheet, row, 0, designator);
		Excel.cell(sheet, row, 1, value);
		return row + 1;
	}

	private int line(Sheet sheet, int row, String designator, double value) {
		Excel.cell(sheet, row, 0, designator);
		Excel.cell(sheet, row, 1, value);
		return row + 1;
	}

	private ExcelHeader createFlowHeader(FlowIndex flowIndex) {
		ExcelHeader header = new ExcelHeader();
		header.setHeaders(HEADERS.FLOW.VALUES);

		// create header entries
		List<IExcelHeaderEntry> headerEntries = new ArrayList<>();
		List<FlowInfo> sortedFlows = mapFlowIndices(header, flowIndex);
		for (FlowInfo flowInfo : sortedFlows) {
			headerEntries.add(new FlowHeaderEntry(flowInfo));
		}
		header.setEntries(headerEntries
				.toArray(new IExcelHeaderEntry[headerEntries.size()]));

		return header;
	}

	private ExcelHeader createProductHeader(ProductIndex productIndex) {
		ExcelHeader header = new ExcelHeader();
		header.setHeaders(HEADERS.PRODUCT.VALUES);

		// create header entries
		List<IExcelHeaderEntry> headerEntries = new ArrayList<>();
		List<ProductInfo> sortedProducts = mapProductIndices(header,
				productIndex);
		for (ProductInfo product : sortedProducts) {
			headerEntries.add(new ProductHeaderEntry(product));
		}
		header.setEntries(headerEntries
				.toArray(new IExcelHeaderEntry[headerEntries.size()]));

		return header;
	}

	private ExcelHeader createImpactCategoryHeader(
			Index<ImpactCategoryDescriptor> categoryIndex) {
		ExcelHeader header = new ExcelHeader();
		header.setHeaders(HEADERS.IMPACT_CATEGORY.VALUES);

		// create header entries
		List<IExcelHeaderEntry> headerEntries = new ArrayList<>();
		List<ImpactCategoryDescriptor> sortedCategories = mapImpactCategoryIndices(
				header, categoryIndex);
		for (ImpactCategoryDescriptor category : sortedCategories) {
			headerEntries.add(new ImpactCategoryHeaderEntry(impactMethod
					.getName(), category));
		}
		header.setEntries(headerEntries
				.toArray(new IExcelHeaderEntry[headerEntries.size()]));

		return header;
	}

	private void createElementarySheet(Workbook workbook) {
		ExcelHeader columnHeader = createProductHeader(inventoryMatrix
				.getProductIndex());
		ExcelHeader rowHeader = createFlowHeader(inventoryMatrix.getFlowIndex());

		MatrixExcelExport export = new MatrixExcelExport();
		export.setColumnHeader(columnHeader);
		export.setRowHeader(rowHeader);
		export.setMatrix(inventoryMatrix.getInterventionMatrix());
		export.writeTo(workbook);
	}

	private void createProductSheet(Workbook workbook) {
		ExcelHeader columnHeader = createProductHeader(inventoryMatrix
				.getProductIndex());
		ExcelHeader rowHeader = createProductHeader(inventoryMatrix
				.getProductIndex());

		MatrixExcelExport export = new MatrixExcelExport();
		export.setColumnHeader(columnHeader);
		export.setRowHeader(rowHeader);
		export.setMatrix(inventoryMatrix.getTechnologyMatrix());
		Sheet sheet = export.writeTo(workbook);
		// set bold style on reference process
		int columnOffSet = rowHeader.getHeaderSize() + 1;
		for (int i = 0; i < columnHeader.getHeaderSize(); i++) {
			Excel.headerStyle(workbook, sheet, i, columnOffSet);
		}
	}

	private void createImpactMethodSheet(Workbook workbook) {
		ExcelHeader columnHeader = createImpactCategoryHeader(impactMatrix
				.getCategoryIndex());
		ExcelHeader rowHeader = createFlowHeader(impactMatrix.getFlowIndex());

		MatrixExcelExport export = new MatrixExcelExport();
		export.setColumnHeader(columnHeader);
		export.setRowHeader(rowHeader);
		export.setMatrix(conjugate(impactMatrix.getValues()));
		export.writeTo(workbook);
	}

	private List<FlowInfo> mapFlowIndices(ExcelHeader header,
			FlowIndex flowIndex) {
		// sort header
		List<FlowInfo> flows = new ArrayList<>();
		List<FlowInfo> sortedFlows = new ArrayList<>();
		for (Flow flow : flowIndex.getFlows()) {
			FlowInfo flowInfo = flowInfoDao.fromFlow(flow);
			flows.add(flowInfo);
			sortedFlows.add(flowInfo);
		}
		Collections.sort(sortedFlows);
		// map indices
		int counter = 0;
		for (FlowInfo flow : sortedFlows) {
			header.putIndexMapping(counter, flows.indexOf(flow));
			counter++;
		}
		return sortedFlows;
	}

	private List<ProductInfo> mapProductIndices(ExcelHeader header,
			ProductIndex productIndex) {
		// sort header
		List<ProductInfo> products = new ArrayList<>();
		List<ProductInfo> sortedProducts = new ArrayList<>();
		for (int index = 0; index < productIndex.getProductIds().size(); index++) {
			Process process = productIndex.getProcessAt(index);
			Exchange product = productIndex.getProductAt(index);
			ProductInfo productInfo = productInfoDao.fromProduct(process,
					product);
			products.add(productInfo);
			sortedProducts.add(productInfo);
		}
		Collections.sort(sortedProducts);
		// put reference products at the beginning
		ProductInfo[] tmp = sortedProducts
				.toArray(new ProductInfo[sortedProducts.size()]);
		int refCounter = 0;
		for (ProductInfo product : tmp) {
			if (product.getProcessId().equals(
					productSystem.getReferenceProcess().getId())) {
				sortedProducts.remove(product);
				String refExId = productSystem.getReferenceExchange().getId();
				int index = product.getId().equals(refExId) ? 0 : refCounter;
				sortedProducts.add(index, product);
				refCounter++;
			}
		}
		// map indices
		int counter = 0;
		for (ProductInfo product : sortedProducts) {
			header.putIndexMapping(counter, products.indexOf(product));
			counter++;
		}
		return sortedProducts;
	}

	private List<ImpactCategoryDescriptor> mapImpactCategoryIndices(
			ExcelHeader header, Index<ImpactCategoryDescriptor> categoryIndex) {
		// sort header
		List<ImpactCategoryDescriptor> categories = new ArrayList<>();
		List<ImpactCategoryDescriptor> sortedCategories = new ArrayList<>();
		for (ImpactCategoryDescriptor category : categoryIndex.getItems()) {
			categories.add(category);
			sortedCategories.add(category);
		}
		Collections.sort(sortedCategories);
		// map indices
		int counter = 0;
		for (ImpactCategoryDescriptor category : sortedCategories) {
			header.putIndexMapping(counter, categories.indexOf(category));
			counter++;
		}
		return sortedCategories;
	}

	private IMatrix conjugate(IMatrix matrix) {
		IMatrix result = MatrixFactory.create(matrix.getColumnDimension(),
				matrix.getRowDimension());
		for (int row = 0; row < matrix.getRowDimension(); row++) {
			for (int column = 0; column < matrix.getColumnDimension(); column++) {
				double value = matrix.getEntry(row, column);
				result.setEntry(column, row, value);
			}
		}
		return result;
	}

	private void writeToFile(Workbook workbook, File file) throws IOException {
		int i = 1;
		File actFile = new File(file.getAbsolutePath());
		while (actFile.exists()) {
			String tmp = file.getAbsolutePath();
			tmp = tmp.substring(0, tmp.lastIndexOf('.')) + "(" + i + ")"
					+ tmp.substring(tmp.lastIndexOf('.'));
			actFile = new File(tmp);
			i++;
		}
		actFile.createNewFile();

		try (FileOutputStream fos = new FileOutputStream(actFile)) {
			workbook.write(fos);
		}
	}

	private interface TITLES {

		String MAIN_TITLE = "OpenLCA Life Cycle Assessment Matrix Export";

		String ELEMENTARY = "Elementary Flows Associated with Processes/Activities, no allocation applied";
		String ELEMENTARY_ALLOCATED = "Elementary Flows Associated with Processes/Activities, after allocation";
		String PRODUCT = "Use of products/services by processes/activities without allocation or co-product/avoided production credits";
		String PRODUCT_ALLOCATED = "Use of Products/Services by Processes/Activities with user-specified allocation or co-product/avoided production applied";
		String IMPACT_FACTORS = "Life Cycle Impact Assessment, Characterization Factors";

	}

	private interface FILE_NAMES {

		String ELEMENTARY = "ElementaryFlowsUnallocated.xlsx";
		String ELEMENTARY_ALLOCATED = "ElementaryFlowsAllocated.xlsx";
		String PRODUCT = "ProductFlowsUnallocated.xlsx";
		String PRODUCT_ALLOCATED = "ProductFlowsAllocated.xlsx";
		String IMPACT_FACTORS = "ImpactFactors.xlsx";

	}

	private interface HEADERS {

		interface FLOW {

			String CATEGORY = "Category";
			String LOCATION = "Elementary flow location";
			String NAME = "Elementary flowname";
			String SUB_CATEGORY = "Sub category";
			String UNIT = "Unit";
			String UUID = "UUID";

			String[] VALUES = new String[] { UUID, CATEGORY, SUB_CATEGORY,
					NAME, LOCATION, UNIT };

		}

		interface PRODUCT {

			String INFRASTRUCTURE_PRODUCT = "Infrastructure product";
			String MULTI_OUTPUT = "Multi-Output process";
			String PROCESS_CATEGORY = "Process category";
			String PROCESS_LOCATION = "Process location";
			String PROCESS_NAME = "Process name";
			String PROCESS_SUB_CATEGORY = "Process sub category";
			String PRODUCT_NAME = "Product name";
			String PRODUCT_UNIT = "Product/Service unit";
			String UUID = "UUID";

			String[] VALUES = new String[] { PROCESS_NAME, PRODUCT_NAME,
					MULTI_OUTPUT, UUID, INFRASTRUCTURE_PRODUCT,
					PROCESS_LOCATION, PROCESS_CATEGORY, PROCESS_SUB_CATEGORY,
					PRODUCT_UNIT };

		}

		interface IMPACT_CATEGORY {

			String CATEGORY = "Sub category";
			String METHOD = "Category";
			String UNIT = "Unit";
			String UUID = "UUID";

			String[] VALUES = new String[] { UUID, CATEGORY, METHOD, UNIT };

		}

	}

	private class FlowHeaderEntry implements IExcelHeaderEntry {

		private FlowInfo flowInfo;

		private FlowHeaderEntry(FlowInfo flowInfo) {
			this.flowInfo = flowInfo;
		}

		@Override
		public String getValue(int count) {
			if (count > HEADERS.FLOW.VALUES.length)
				return null;
			String header = HEADERS.FLOW.VALUES[count];
			return getValue(header);
		}

		private String getValue(String header) {
			switch (header) {
			case HEADERS.FLOW.NAME:
				return flowInfo.getName();
			case HEADERS.FLOW.UUID:
				return flowInfo.getId();
			case HEADERS.FLOW.LOCATION:
				return flowInfo.getLocation();
			case HEADERS.FLOW.CATEGORY:
				return flowInfo.getCategory();
			case HEADERS.FLOW.SUB_CATEGORY:
				return flowInfo.getSubCategory();
			case HEADERS.FLOW.UNIT:
				return flowInfo.getUnit();
			}
			return null;
		}

	}

	private class ProductHeaderEntry implements IExcelHeaderEntry {

		private ProductInfo productInfo;

		private ProductHeaderEntry(ProductInfo productInfo) {
			this.productInfo = productInfo;
		}

		@Override
		public String getValue(int count) {
			if (count > HEADERS.PRODUCT.VALUES.length)
				return null;
			String header = HEADERS.PRODUCT.VALUES[count];
			return getValue(header);
		}

		private String getValue(String header) {
			switch (header) {
			case HEADERS.PRODUCT.PROCESS_NAME:
				return productInfo.getProcess();
			case HEADERS.PRODUCT.PRODUCT_NAME:
				return productInfo.getProduct();
			case HEADERS.PRODUCT.MULTI_OUTPUT:
				return Boolean.toString(productInfo.isFromMultiOutputProcess());
			case HEADERS.PRODUCT.UUID:
				return productInfo.getProductId();
			case HEADERS.PRODUCT.INFRASTRUCTURE_PRODUCT:
				return Boolean.toString(productInfo
						.isFromInfrastructureProcess());
			case HEADERS.PRODUCT.PROCESS_LOCATION:
				return productInfo.getProcessLocation();
			case HEADERS.PRODUCT.PROCESS_CATEGORY:
				return productInfo.getProcessCategory();
			case HEADERS.PRODUCT.PROCESS_SUB_CATEGORY:
				return productInfo.getProcessSubCategory();
			case HEADERS.PRODUCT.PRODUCT_UNIT:
				return productInfo.getProductUnit();
			}
			return null;
		}

	}

	private class ImpactCategoryHeaderEntry implements IExcelHeaderEntry {

		private ImpactCategoryDescriptor impactCategory;
		private String methodName;

		private ImpactCategoryHeaderEntry(String methodName,
				ImpactCategoryDescriptor impactCategory) {
			this.methodName = methodName;
			this.impactCategory = impactCategory;
		}

		@Override
		public String getValue(int count) {
			if (count > HEADERS.IMPACT_CATEGORY.VALUES.length)
				return null;
			String header = HEADERS.IMPACT_CATEGORY.VALUES[count];
			return getValue(header);
		}

		private String getValue(String header) {
			switch (header) {
			case HEADERS.IMPACT_CATEGORY.CATEGORY:
				return impactCategory.getName();
			case HEADERS.IMPACT_CATEGORY.UUID:
				return impactCategory.getRefId();
			case HEADERS.IMPACT_CATEGORY.METHOD:
				return methodName;
			case HEADERS.IMPACT_CATEGORY.UNIT:
				return impactCategory.getReferenceUnit();
			}
			return null;
		}

	}

}
