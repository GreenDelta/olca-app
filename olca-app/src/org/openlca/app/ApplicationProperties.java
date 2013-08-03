/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.PreferenceStore;
import org.openlca.app.preferencepages.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class provides the openLCA properties
 */
// TODO: why a separate preference store?
// TODO: merge with preferences
public enum ApplicationProperties {

	/**
	 * Default allocation method for calculating a product system
	 */
	PROP_DEFAULT_ALLOCATION_METHOD("dam"),

	/**
	 * Default calculation method
	 */
	PROP_DEFAULT_CALCULATION_METHOD("dcm"),

	/**
	 * Default LCIA method for calcualting LCIA results of a product system
	 */
	PROP_DEFAULT_LCIA_METHOD("dlcia"),

	/**
	 * Default Normalization and weighting set for calcualting LCIA results of a
	 * product system
	 */
	PROP_DEFAULT_NORMALIZATION_WEIGHTING_SET("dnws"),

	/**
	 * Last selected export directory
	 */
	PROP_EXPORT_DIRECTORY("expdir"),

	/**
	 * Last selected import directory
	 */
	PROP_IMPORT_DIRECTORY("impdir"),

	/**
	 * Memory usage
	 */
	PROP_MEMORY("Xmx"),

	/**
	 * Platform language
	 */
	PROP_NATIONAL_LANGUAGE("nl"),

	/**
	 * Show outline
	 */
	PROP_OUTLINE("outline"),

	/**
	 * Sort direction of parameters
	 */
	PROP_PARAMETER_SORT_DIRECTION("psd");

	private static Logger log = LoggerFactory
			.getLogger(ApplicationProperties.class);

	/**
	 * The file where the system properties are saved
	 */
	private final static File iniFile = new File(Platform.getInstanceLocation()
			.getURL().getFile(), "openLCA.ini");

	/**
	 * The openLCA preference store
	 */
	private static PreferenceStore preferenceStore;

	/**
	 * Value for bottom up sort direction. For example: If a parameter p0 is
	 * defined twice, once in a process and once as a database parameter, the
	 * local process parameter will be used for calculations
	 */
	public final static int BOTTOM_UP = 1;

	/**
	 * Value for top down sort direction. For example: If a parameter p0 is
	 * defined twice, once in a process and once as a database parameter, the
	 * database wide parameter will be used for calculations
	 */
	public final static int TOP_DOWN = 0;

	/**
	 * Key of the property (property name in ini file)
	 */
	private String key;

	/**
	 * Private constructor
	 * 
	 * @param key
	 *            The key of the property
	 */
	private ApplicationProperties(final String key) {
		this.key = key;
	}

	/**
	 * Loads the default value for the given property
	 * 
	 * @param property
	 *            The property the default value is requested for
	 * @return The default value of the given property
	 */
	private static String getDefaultValue(final String property) {
		String defaultValue = null;
		if (property.equals(PROP_PARAMETER_SORT_DIRECTION.key)) {
			// sort direction default value = top down
			defaultValue = Integer.toString(TOP_DOWN);
		} else if (property.equals(PROP_NATIONAL_LANGUAGE.key)) {
			// english
			defaultValue = Language.ENGLISH.getCode();
		} else if (property.equals(PROP_DEFAULT_CALCULATION_METHOD.key)) {
			// Matrix solver as default calculation method
			defaultValue = "org.openlca.core.calculation.matrix.solver.MatrixSolver";
		} else if (property.equals(PROP_MEMORY.key)) {
			// 512 MB is default value
			defaultValue = "512";
		}
		return defaultValue;
	}

	/**
	 * Loads the value of the property in the preferences store
	 * 
	 * @param property
	 *            The property key
	 * @return The value of the property
	 */
	private static String getPropertyValue(final String property) {
		return getPreferenceStore().getString(property);
	}

	/**
	 * Saves the system properties into the ini file
	 * 
	 */
	private static void saveSystemPropertyValues() {
		try (OutputStreamWriter stream = new OutputStreamWriter(
				new FileOutputStream(iniFile));
				BufferedWriter writer = new BufferedWriter(stream)) {
			if (!iniFile.exists()) {
				iniFile.createNewFile();
			}
			// write to file

			// write national language
			writer.write("-nl");
			writer.newLine();
			writer.write(PROP_NATIONAL_LANGUAGE.getValue());
			writer.newLine();

			// write maximum memory usage
			writer.write("-vmargs");
			writer.newLine();
			writer.write("-Xmx" + PROP_MEMORY.getValue() + "M");
			writer.newLine();

		} catch (final IOException e) {
			log.error("Save system properties into ini file failed", e);
		}
	}

	/**
	 * Stores the new value of the property in the preferences store
	 * 
	 * @param property
	 *            The property key
	 * @param value
	 *            The new value of the property
	 */
	private static void setPropertyValue(final String property,
			final String value) {
		getPreferenceStore().setValue(property, value != null ? value : "");
		try {
			getPreferenceStore().save();
		} catch (final IOException e) {
			log.error("Save property failed", e);
		}
		if (property.equals(PROP_NATIONAL_LANGUAGE.key)
				|| property.equals(PROP_MEMORY.key)) {
			saveSystemPropertyValues();
		}
	}

	/**
	 * Loads the preference store. If no 'preferences.properties' file exists in
	 * the install location a new one will be created
	 * 
	 * @return The openLCA preference store
	 */
	public static PreferenceStore getPreferenceStore() {
		if (preferenceStore == null) {
			// load the preference store
			final File file = new File(Platform.getInstanceLocation().getURL()
					.getFile()
					+ "/preferences/preferences.properties");
			if (!file.exists()) {
				// create parent directories and file
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				try {
					file.createNewFile();
				} catch (final IOException e) {
					log.error("Create file failed", e);
				}
			}
			preferenceStore = new PreferenceStore(file.getAbsolutePath());
			try {
				// load preference store
				preferenceStore.load();
			} catch (final IOException e1) {
				log.error("loading preference store failed", e1);
			}
		}
		return preferenceStore;
	}

	/**
	 * Getter of the default value
	 * 
	 * @return The default value of the property
	 */
	public String getDefaultValue() {
		return getDefaultValue(key);
	}

	/**
	 * Loads the value for the given property
	 * 
	 * @return The value of the given property
	 */
	public String getValue() {
		String value = getPropertyValue(key);
		if (value == null || value.equals("")) {
			value = getDefaultValue(key);
		}
		return value;
	}

	/**
	 * Loads a value for the property with the given additional argument
	 * 
	 * @param argument
	 *            The additional argument
	 * @return The value for the property for the additional argument
	 */
	public String getValue(final String argument) {
		String value = getPropertyValue(key + argument);
		if (value == null || value.equals("")) {
			value = getDefaultValue(key);
		}
		return value;
	}

	/**
	 * Saves the given property/value pair into the ini file
	 * 
	 * @param value
	 *            The new value of the given property
	 */
	public void setValue(final String value) {
		setPropertyValue(key, value);
	}

	/**
	 * Saves the given property/value pair into the ini file
	 * 
	 * @param value
	 *            The new value of the given property
	 * @param argument
	 *            An additional argument
	 */
	public void setValue(final String value, final String argument) {
		setPropertyValue(key + argument, value);
	}

}
