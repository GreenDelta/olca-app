package org.openlca.ilcd.network.rcp.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.openlca.ilcd.network.rcp.ui.messages"; //$NON-NLS-1$

	// ILCD network connection
	public static String URL;
	public static String USER;
	public static String PASSWORD;
	public static String CONNECTION;

	// command names
	public static String CHANGE;

	public static String SearchErrorMessage;
	public static String SearchFailedMessage;
	public static String SearchPageDescription;
	public static String NetworkExport;
	public static String NetworkImport;
	public static String NetworkSearch;
	public static String Process;
	public static String RunImport;
	public static String Search;

	// SearchResultViewer
	public static String Name;
	public static String Location;
	public static String Time;
	public static String Type;

	// dialog messages
	public static String CONNECTION_FAILED_MSG;
	public static String AUTHENTICATION_FAILED_MSG;
	public static String NO_READ_OR_WRITE_ACCESS_MSG;
	public static String CONNECTION_WORKS_MSG;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
