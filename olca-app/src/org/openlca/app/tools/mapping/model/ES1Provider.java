package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openlca.core.database.IDatabase;
import org.openlca.core.io.maps.FlowRef;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.UnitDescriptor;
import org.openlca.ecospold.IEcoSpold;
import org.openlca.ecospold.IExchange;
import org.openlca.ecospold.io.DataSetType;
import org.openlca.ecospold.io.EcoSpold;
import org.openlca.io.ecospold1.input.ES1KeyGen;
import org.openlca.util.Strings;
import org.openlca.util.ZipFiles;
import org.slf4j.LoggerFactory;

public class ES1Provider implements FlowProvider {

	private final File file;
	private List<FlowRef> refs;

	private ES1Provider(File file) {
		this.file = Objects.requireNonNull(file);
	}

	public static ES1Provider of(File file) {
		return new ES1Provider(file);
	}

	public File file() {
		return file;
	}

	@Override
	public List<FlowRef> getFlowRefs() {
		if (refs != null)
			return refs;
		refs = new ArrayList<>();
		var handled = new HashSet<String>();
		try {
			scan(spold -> {
				for (var ds : spold.getDataset()) {
					for (var flows : ds.getFlowData()) {
						for (var exchange : flows.getExchange()) {
							if (!exchange.isElementaryFlow())
								continue;
							var refId = ES1KeyGen.forFlow(exchange);
							if (handled.contains(refId))
								continue;
							handled.add(refId);
							var ref = refOf(refId, exchange);
							refs.add(ref);
						}
					}
				}
			});
		} catch (IOException e) {
			var log = LoggerFactory.getLogger(getClass());
			log.error("failed to get flow refs", e);
		}
		return refs;
	}

	private FlowRef refOf(String refId, IExchange e) {
		var ref = new FlowRef();
		var flow = new FlowDescriptor();
		flow.refId = refId;
		flow.name = e.getName();
		flow.flowType = e.isElementaryFlow()
				? FlowType.ELEMENTARY_FLOW
				: FlowType.PRODUCT_FLOW;
		ref.flowCategory = e.getCategory();
		if (Strings.notEmpty(e.getSubCategory())) {
			ref.flowCategory += "/" + e.getSubCategory();
		}
		ref.flow = flow;

		ref.unit = new UnitDescriptor();
		ref.unit.name = e.getUnit();
		if (Strings.notEmpty(e.getLocation())) {
			ref.flowLocation = e.getLocation();
		}
		return ref;
	}

	private void scan(Consumer<IEcoSpold> fn) throws IOException {
		var fileName = file.getName().toLowerCase();
		if (fileName.endsWith(".zip")) {
			try (var zip = ZipFiles.open(file)) {
				var entries = zip.entries();
				while (entries.hasMoreElements()) {
					var entry = entries.nextElement();
					var type = typeOf(zip, entry);
					if (type == null)
						continue;
					try (var stream = zip.getInputStream(entry)) {
						var spold = EcoSpold.read(stream, type);
						fn.accept(spold);
					}
				}
			}
		} else {
			var type = EcoSpold.typeOf(file).orElse(null);
			if (type == null)
				return;
			var spold = EcoSpold.read(file, type);
			fn.accept(spold);
		}
	}

	private DataSetType typeOf(ZipFile zip, ZipEntry entry) {
		if (entry == null || entry.isDirectory())
			return null;
		var name = entry.getName().toLowerCase();
		if (!name.endsWith(".xml"))
			return null;
		try (var stream = zip.getInputStream(entry)) {
			return EcoSpold.typeOf(stream).orElse(null);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public void persist(List<FlowRef> refs, IDatabase db) {
	}

	@Override
	public void sync(Stream<FlowRef> externalRefs) {
		Sync.packageSync(this, externalRefs);
	}
}
