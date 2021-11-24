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
		if (format != null) {
			switch (format) {
				case JSON_LD_ZIP:
					return JSON_ZIP;
				case ILCD_ZIP:
					return ILCD_ZIP;
				case SIMAPRO_CSV:
					return SIMAPRO_CSV;
				default:
					break;
			}
		}

		String fname = file.getName().toLowerCase();
		return fname.endsWith(".csv")
			? MAPPING_CSV // TODO: we should check the format here
			: UNKNOWN;
	}
}
