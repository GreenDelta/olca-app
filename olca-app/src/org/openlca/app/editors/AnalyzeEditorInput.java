/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.app.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * Input for the analysis editor with a cache key for the calculation setup and
 * result.
 */
public class AnalyzeEditorInput implements IEditorInput {

	private String setupKey;
	private String resultKey;

	public AnalyzeEditorInput(String setupKey, String resultKey) {
		this.setupKey = setupKey;
		this.resultKey = resultKey;
	}

	public String getResultKey() {
		return resultKey;
	}

	public String getSetupKey() {
		return setupKey;
	}

	@Override
	public boolean exists() {
		return resultKey != null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return "";
	}

}
