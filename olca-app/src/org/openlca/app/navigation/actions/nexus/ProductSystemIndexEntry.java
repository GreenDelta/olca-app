package org.openlca.app.navigation.actions.nexus;

import java.util.Set;

import org.openlca.app.navigation.actions.nexus.Types.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Version;

class ProductSystemIndexEntry extends IndexEntry {

	int numberOfProcesses;
	String quantitativeReference;
	double unitProcessPercentage;
	String dqTime;

	ProductSystemIndexEntry(ProductSystem system, MetaData metaData, Set<Long> unitProcesses) {
		super(metaData);
		this.type = ModelType.PRODUCT_SYSTEM;
		this.refId = system.refId;
		this.name = system.name;
		this.description = system.description;
		this.version = Version.asString(system.version);
		this.validFrom = null;
		this.validUntil = null;
		this.numberOfProcesses = system.processes.size();
		this.quantitativeReference = system.referenceExchange.flow.name;
		this.unitProcessPercentage = (double) system.processes.stream().filter(unitProcesses::contains).count()
				/ system.processes.size();
		this.dqTime = null;
	}

}