package org.openlca.app.navigation.actions.nexus;

import java.util.Date;
import java.util.List;

import org.openlca.app.navigation.actions.nexus.Types.ModelType;
import org.openlca.app.navigation.actions.nexus.Types.ProcessType;
import org.openlca.core.model.Process;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Version;

class ProcessIndexEntry extends IndexEntry {

	String category;
	String technology;
	String copyrightHolder;
	String location;
	double latitude;
	double longitude;
	String contact;
	String documentor;
	String generator;
	Date creationDate;
	String unspscCode;
	String co2peCode;
	List<String> reviewers;
	double completeness;
	double amountDeviation;
	double representativenessValue;
	boolean copyrightProtected;
	List<ProcessType> processType;

	ProcessIndexEntry(Process process, MetaData metaData) {
		super(metaData);
		this.type = ModelType.PROCESS;
		this.refId = process.refId;
		this.name = process.name;
		this.description = process.description;
		this.version = process.version != 0l ? Version.asString(process.version) : "";
		this.validFrom = process.documentation.validFrom;
		this.validUntil = process.documentation.validUntil;
		if (process.category != null) {
			category = process.category.toPath();
		}
		technology = process.documentation.technology;
		copyrightHolder = getName(process.documentation.dataSetOwner);
		if (process.location != null) {
			location = process.location.name;
			latitude = process.location.latitude;
			longitude = process.location.longitude;
		}
		contact = getName(process.documentation.dataSetOwner);
		documentor = getName(process.documentation.dataDocumentor);
		generator = getName(process.documentation.dataGenerator);
		creationDate = process.documentation.creationDate;
		copyrightProtected = process.documentation.copyright;
	}

	private static String getName(RootEntity entity) {
		return entity != null ? entity.name : null;
	}

}
