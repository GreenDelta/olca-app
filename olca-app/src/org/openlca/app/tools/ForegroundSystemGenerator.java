package org.openlca.app.tools;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.slf4j.LoggerFactory;

/**
 * This class generates random foreground systems on a given database. It is
 * used to test the library feature in openLCA and can be removed again when
 * we are ready with this. It is not part of the API and should be just used
 * for tests.
 */
public class ForegroundSystemGenerator implements Runnable {

	private final IDatabase db;
	private final int size;

	public ForegroundSystemGenerator(IDatabase db, int size) {
		this.db = db;
		this.size = size;
	}

	@Override
	public void run() {
		if (size < 1)
			return;

		// find the quantity
		var log = LoggerFactory.getLogger(getClass());
		var quantity = selectQuantity();
		if (quantity.isEmpty()) {
			log.error("could not find a flow property");
			return;
		}

		// prepare the categories
		var catDao = new CategoryDao(db);
		var productCat = catDao.sync(ModelType.FLOW, "_random");
		var processCat = catDao.sync(ModelType.PROCESS, "_random");

		// create products
		var products = new Flow[size];
		var flowDao = new FlowDao(db);
		for (int i = 0; i < products.length; i++) {
			var product = Flow.product(
					name("product", i),
					quantity.get());
			product.category = productCat;
			products[i] = flowDao.insert(product);
		}
		log.info("created {} products", size);

		// prepare the foreground processes
		var processes = new Process[size];
		for (int i = 0; i < processes.length; i++) {
			var process = Process.of(
					name("process", i), products[i]);
			process.category = processCat;
			processes[i] = process;
		}

		// generate a tree, the root is the reference
		// process
		var rand = new Random();
		var levels = new ArrayList<Process[]>();
		levels.add(new Process[]{processes[0]});
		var offset = 1;
		var depth = 1;
		while (offset < size) {
			var rest = size - offset;
			var levelSize = rest < 10
					? rest
					: rand.nextInt(10) + 1;
			var level = new Process[levelSize];
			System.arraycopy(processes, offset, level, 0, levelSize);
			levels.add(level);

			// link this level with the previous level
			var previous = levels.get(depth - 1);
			for (var provider : level) {
				var recipient = previous[rand.nextInt(previous.length)];
				recipient.input(
						provider.quantitativeReference.flow,
						rand.nextInt(5) + 5);
			}

			offset += levelSize;
			depth++;
		}

		// save the foreground system
		var processDao = new ProcessDao(db);
		for (var process : processes) {
			processDao.insert(process);
		}
		log.info("created {} processes", size);
	}

	private String name(String prefix, int i) {
		int dim = 1 + ((int) Math.log10(size));
		var pattern = prefix + "_%0" + dim + "d";
		return String.format(pattern, i + 1);
	}

	private Optional<FlowProperty> selectQuantity() {
		var props = new FlowPropertyDao(db).getAll();
		if (props.isEmpty())
			return Optional.empty();
		var mass = props.stream()
				.filter(prop -> {
					if (prop.unitGroup == null)
						return false;
					var unit = prop.unitGroup.referenceUnit;
					return unit != null && "kg".equals(unit.name);
				})
				.findAny();
		return mass.isPresent()
				? mass
				: Optional.of(props.get(0));
	}

}
