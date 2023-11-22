package org.openlca.app.navigation.actions.nexus;

import java.util.Date;

import org.openlca.app.navigation.actions.nexus.Types.ModelType;

class IndexEntry {

	ModelType type;
	String refId;
	String name;
	String description;
	String version;
	Date validFrom;
	Date validUntil;
	final MetaData metaData;
	
	IndexEntry(MetaData metaData) {
		this.metaData = metaData;
	}
	
}
