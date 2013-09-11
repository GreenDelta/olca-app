package org.openlca.core.editors.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

import org.openlca.core.database.FlowDao;
import org.openlca.core.math.IMatrix;
import org.openlca.core.matrices.FlowIndex;
import org.openlca.core.matrices.InventoryMatrix;
import org.openlca.core.matrices.ProductIndex;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes a product system as matrices into CSV files.
 */
public class MatrixExport implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());
	private MatrixExportData data;
	private FlowDao flowDao;
	private String separator;
	private String point;
	private HashMap<String, String> categoryCache = new HashMap<>();

	public MatrixExport(MatrixExportData data) {
		this.data = data;
		this.flowDao = new FlowDao(data.getDatabase());
		separator = data.getColumnSeperator();
		point = data.getDecimalSeparator();
	}

	@Override
	public void run() {
		log.trace("Run matrix export");
		if (data == null || !data.valid()) {
			log.error("Invalid export data {}", data);
			return;
		}
		log.trace("Build inventory matrix");
		InventoryMatrixBuilder matrixBuilder = new InventoryMatrixBuilder(
				data.getProductSystem());
		InventoryMatrix matrix = matrixBuilder.build();
		AllocationMatrix allocationMatrix = AllocationMatrix.create(matrix,
				data.getProductSystem(), data.getDatabase());
		allocationMatrix.apply(matrix);
		log.trace("Write technology matrix");
		writeTechFile(matrix);
		log.trace("Write intervention matrix");
		writeEnviFile(matrix);
		log.trace("Export done");
	}

	private void writeTechFile(InventoryMatrix inventory) {
		try (FileWriter writer = new FileWriter(data.getTechnologyFile());
				BufferedWriter buffer = new BufferedWriter(writer)) {
			writeTechMatrix(inventory, buffer);
		} catch (Exception e) {
			log.error("Failed to write technology matrix", e);
		}
	}

	private void writeTechMatrix(InventoryMatrix inventory,
			BufferedWriter buffer) throws Exception {
		IMatrix matrix = inventory.getTechnologyMatrix();
		ProductIndex productIndex = inventory.getProductIndex();
		int size = productIndex.size();
		for (int row = 0; row < size; row++) {
			Exchange exchange = productIndex.getProductAt(row);
			writeName(exchange, buffer);
			sep(buffer);
			writeCategory(exchange, buffer);
			sep(buffer);
			for (int col = 0; col < size; col++) {
				double val = matrix.getEntry(row, col);
				writeValue(val, buffer);
				sep(buffer, col, size);
			}
			buffer.newLine();
		}
	}

	private void writeEnviFile(InventoryMatrix matrix) {
		try (FileWriter writer = new FileWriter(data.getInterventionFile());
				BufferedWriter buffer = new BufferedWriter(writer)) {
			writeEnviMatrix(matrix, buffer);
		} catch (Exception e) {
			log.error("Failed to write intervention matrix", e);
		}
	}

	private void writeEnviMatrix(InventoryMatrix inventory,
			BufferedWriter buffer) throws Exception {
		ProductIndex productIndex = inventory.getProductIndex();
		FlowIndex flowIndex = inventory.getFlowIndex();
		int rows = flowIndex.size();
		int columns = productIndex.size();
		writeEnviMatrixHeader(buffer, productIndex);
		IMatrix matrix = inventory.getInterventionMatrix();
		for (int row = 0; row < rows; row++) {
			Flow flow = flowIndex.getFlowAt(row);
			writeName(flow, buffer);
			sep(buffer);
			writeCategory(flow, buffer);
			sep(buffer);
			for (int col = 0; col < columns; col++) {
				double val = matrix.getEntry(row, col);
				writeValue(val, buffer);
				sep(buffer, col, columns);
			}
			buffer.newLine();
		}

	}

	private void writeEnviMatrixHeader(BufferedWriter buffer,
			ProductIndex productIndex) throws Exception, IOException {
		sep(buffer);
		sep(buffer);
		int columns = productIndex.size();
		for (int col = 0; col < columns; col++) {
			Exchange product = productIndex.getProductAt(col);
			writeName(product, buffer);
			sep(buffer, col, columns);
		}
		buffer.newLine();
		sep(buffer);
		sep(buffer);
		for (int col = 0; col < columns; col++) {
			Exchange product = productIndex.getProductAt(col);
			writeCategory(product, buffer);
			sep(buffer, col, columns);
		}
		buffer.newLine();
	}

	private void writeName(Exchange exchange, Writer buffer) throws Exception {
		if (exchange == null)
			return;
		Flow flow = exchange.getFlow();
		writeName(flow, buffer);
	}

	private void writeName(Flow flow, Writer buffer) {
		if (flow == null)
			return;
		String name = flow.getName();
		try {
			String unit = flowDao.getRefUnitName(flow);
			name = name.concat(" [").concat(unit).concat("]");
			quote(name, buffer);
		} catch (Exception e) {
			log.error("Failed to load ref-unit", e);
			return;
		}
	}

	private void writeValue(double d, Writer buffer) throws Exception {
		String number = Double.toString(d);
		if (!point.equals("."))
			number = number.replace(".", point);
		buffer.write(number);
	}

	private void quote(String val, Writer buffer) throws Exception {
		buffer.write('"');
		buffer.write(val);
		buffer.write('"');
	}

	private void sep(Writer buffer) throws Exception {
		buffer.append(separator);
	}

	private void sep(Writer buffer, int position, int dimension)
			throws Exception {
		if (position < dimension - 1)
			sep(buffer);
	}

	private void writeCategory(Exchange exchange, Writer buffer)
			throws Exception {
		if (exchange == null || exchange.getFlow() == null)
			return;
		writeCategory(exchange.getFlow(), buffer);
	}

	private void writeCategory(Flow flow, Writer buffer) throws Exception {
		if (flow == null || flow.getCategoryId() == null)
			return;
		String catId = flow.getCategoryId();
		String catPath = categoryCache.get(catId);
		if (catPath == null) {
			catPath = CategoryPath.getShort(catId, data.getDatabase());
			categoryCache.put(catId, catPath);
		}
		quote(catPath, buffer);
	}

}
