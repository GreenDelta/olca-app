package org.openlca.core.application;

/**
 * openLCA specific arguments that can be passed to the application via the
 * command line or the openLCA.ini file.
 */
public enum CommandArgument {

	/**
	 * olcaLog: a start log level for the application (all, info, warn, or
	 * error).
	 */
	LOG_LEVEL("olcaLog"),

	/**
	 * olcaDataDir: a full path to a directory folder with write permissions
	 * that will be used as workspace directory.
	 */
	DATA_DIR("olcaDataDir"),

	/**
	 * olcaVersion: the openLCA version which was set in the build.
	 */
	VERSION("olcaVersion");

	private final String key;

	private CommandArgument(String key) {
		this.key = key;
	}

	/**
	 * Get the value of the argument that was passed to the application. Returns
	 * null if the argument is not set or has no value.
	 */
	public String getValue() {
		return EclipseCommandLine.getArg(key);
	}

}
