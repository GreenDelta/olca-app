/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.io;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.openlca.io.ui.messages";

	public static String AlreadyExistsWarning;
	public static String ChooseDirectoryButton;
	public static String ChooseDirectoryLabel;
	public static String ChooseFileNameLabel;
	public static String DirectoryWillBeCreated;
	public static String FileImportPage_ChooseDirectoryButton;
	public static String FileImportPage_ChooseDirectoryLabel;
	public static String FileImportPage_Description;
	public static String FileImportPage_Title;
	public static String SelectDatabasePage_Description;
	public static String SelectDatabasePage_SelectDatabase;
	public static String SelectDirectoryPage_Description;
	public static String SelectDirectoryPage_Title;
	public static String SelectObjectPage_Description;
	public static String SelectObjectPage_Title;
	public static String UnitMappingPage_CheckingUnits;
	public static String UnitMappingPage_Description;
	public static String UnitMappingPage_Title;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
