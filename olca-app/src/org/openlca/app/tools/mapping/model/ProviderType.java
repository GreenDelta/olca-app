package org.openlca.app.tools.mapping.model;

import java.io.File;

import org.openlca.io.Format;

/**
 * An enumeration of possible provider types for mapping definitions.
 */
public enum ProviderType {

	JSON_ZIP,

	ILCD_ZIP,

	SIMAPRO_CSV,

	MAPPING_CSV,

	UNKNOWN;

	/**
	 * Try to determine the provider type from the given file.
	 */
	public static ProviderType of(File file) {
		if (file == null || !file.isFile())
			return UNKNOWN;

		var format = Format.detect(file).orElse(null);
		if (format == null)
			return UNKNOWN;
		return switch (format) {
			case JSON_LD_ZIP -> JSON_ZIP;
			case ILCD_ZIP -> ILCD_ZIP;
			case SIMAPRO_CSV -> SIMAPRO_CSV;
			case MAPPING_CSV -> MAPPING_CSV;
			default -> UNKNOWN;
		};
	}
}
