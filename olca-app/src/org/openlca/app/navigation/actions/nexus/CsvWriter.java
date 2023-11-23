package org.openlca.app.navigation.actions.nexus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.openlca.util.Strings;

class CsvWriter {

	private static final char DELIMITER = '"';
	private static final char SEPARATOR = ';';
	private static final char ARRAY_DELIMITER = ',';
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final String[] HEADERS = { "type", "refId", "name", "description", "version", "validFrom",
			"validUntil", "supportedNomenclatures", "lciaMethods", "modelingType", "multifunctionalModeling",
			"biogenicCarbonModeling", "endOfLifeModeling", "waterModeling", "infrastructureModeling",
			"emissionModeling", "carbonStorageModeling", "reviewType", "reviewSystem", "category", "technology",
			"copyrightHolder", "location", "latitude", "longitude", "contact", "documentor", "generator",
			"creationDate", "unspscCode", "co2peCode", "reviewers", "completeness", "amountDeviation",
			"representativenessValue", "copyrightProtected", "processType", "representativenessType",
			"sourceReliability", "aggregationType", "systemModel", "creator", "numberOfProcesses",
			"quantitativeReference", "intendedAudience", "unitProcessPercentage", "dqTime" };

	static void write(File file, List<IndexEntry> entries) throws IOException {
		try (var writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(Strings.join(HEADERS, SEPARATOR));
			writer.newLine();
			for (var entry : entries) {
				var line = general(entry);
				if (entry instanceof ProcessIndexEntry p) {
					line = process(line, p);
				} else if (entry instanceof ProductSystemIndexEntry p) {
					line = productSystem(line, p);
				}
				writer.write(line);
				writer.newLine();
			}
		}
	}

	private static String general(IndexEntry e) {
		var line = "";
		line = addCell(line, enumValue(e.type));
		line = addCell(line, e.refId);
		line = addCell(line, esc(e.name));
		line = addCell(line, esc(e.description));
		line = addCell(line, e.version);
		line = addCell(line, date(e.validFrom));
		line = addCell(line, date(e.validUntil));
		line = addCell(line, array(e.metaData.supportedNomenclatures));
		line = addCell(line, array(e.metaData.lciaMethods));
		line = addCell(line, enumValues(e.metaData.modelingType));
		line = addCell(line, enumValues(e.metaData.multifunctionalModeling));
		line = addCell(line, enumValues(e.metaData.biogenicCarbonModeling));
		line = addCell(line, enumValues(e.metaData.endOfLifeModeling));
		line = addCell(line, enumValues(e.metaData.waterModeling));
		line = addCell(line, enumValues(e.metaData.infrastructureModeling));
		line = addCell(line, enumValues(e.metaData.emissionModeling));
		line = addCell(line, enumValues(e.metaData.carbonStorageModeling));
		line = addCell(line, enumValues(e.metaData.reviewType));
		line = addCell(line, enumValues(e.metaData.reviewSystem));
		return line;
	}

	private static String process(String line, ProcessIndexEntry e) {
		line = addCell(line, esc(e.category));
		line = addCell(line, esc(e.technology));
		line = addCell(line, esc(e.copyrightHolder));
		line = addCell(line, esc(e.location));
		line = addCell(line, number(e.latitude));
		line = addCell(line, number(e.longitude));
		line = addCell(line, esc(e.contact));
		line = addCell(line, esc(e.documentor));
		line = addCell(line, esc(e.generator));
		line = addCell(line, date(e.creationDate));
		line = addCell(line, esc(e.unspscCode));
		line = addCell(line, esc(e.co2peCode));
		line = addCell(line, array(e.reviewers));
		line = addCell(line, number(e.completeness));
		line = addCell(line, number(e.amountDeviation));
		line = addCell(line, number(e.representativenessValue));
		line = addCell(line, bool(e.copyrightProtected));
		line = addCell(line, enumValues(e.processType));
		line = addCell(line, enumValues(e.metaData.representativenessType));
		line = addCell(line, enumValues(e.metaData.sourceReliability));
		line = addCell(line, enumValues(e.metaData.aggregationType));
		line = addCell(line, array(e.metaData.systemModel));
		return line;
	}

	private static String productSystem(String line, ProductSystemIndexEntry e) {
		for (var i = 0; i < 22; i++) {
			line = addCell(line, "");
		}
		line = addCell(line, esc(e.metaData.creator));
		line = addCell(line, number(e.numberOfProcesses));
		line = addCell(line, esc(e.quantitativeReference));
		line = addCell(line, esc(e.metaData.intendedAudience));
		line = addCell(line, number(e.unitProcessPercentage));
		line = addCell(line, esc(e.dqTime));
		return line;
	}

	private static String addCell(String line, String value) {
		return Strings.nullOrEmpty(line) ? value : line + SEPARATOR + value;
	}

	private static String enumValue(Enum<?> enumValue) {
		return enumValue != null
				? enumValue.name()
				: "";
	}

	private static String enumValues(List<? extends Enum<?>> enumValues) {
		return enumValues != null
				? array(enumValues.stream().map(Enum::name).toList())
				: "";
	}

	private static String date(Date date) {
		return date != null
				? DATE_FORMAT.format(date)
				: "";
	}

	private static String esc(String value) {
		return value != null ? DELIMITER + value.replace(DELIMITER, '\'') + DELIMITER : "";
	}

	private static String number(double v) {
		return v != 0 ? Double.toString(v) : "";
	}

	private static String number(int v) {
		return v != 0 ? Integer.toString(v) : "";
	}

	private static String bool(boolean v) {
		return v ? "TRUE" : "FALSE";
	}

	private static String array(List<String> values) {
		return values != null
				? Strings.join(values, ARRAY_DELIMITER)
				: "";
	}

}
