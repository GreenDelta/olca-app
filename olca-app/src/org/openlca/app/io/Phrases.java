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

public class Phrases extends NLS {

	private static final String BUNDLE_NAME = "org.openlca.io.ui.phrases";

	public static String ConversionFactor;
	public static String FlowProperty;
	public static String ReferenceUnit;
	public static String Unit;
	public static String UnitGroup;
	public static String Processes;
	public static String Flows;
	public static String LCIAMethods;
	public static String UnitGroups;
	public static String FlowProperties;
	public static String Actor;
	public static String Actors;
	public static String Source;
	public static String Sources;
	public static String ProductSystems;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Phrases.class);
	}

	private Phrases() {
	}
}
