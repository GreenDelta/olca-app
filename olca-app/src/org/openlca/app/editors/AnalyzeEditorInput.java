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
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;

/**
 * Input for the analysis editor.
 */
public class AnalyzeEditorInput implements IEditorInput {

	private NormalizationWeightingSet nwSet;
	private String resultKey;
	private IDatabase database;
	private ImpactMethodDescriptor methodDescriptor;

	public NormalizationWeightingSet getNwSet() {
		return nwSet;
	}

	public void setNwSet(NormalizationWeightingSet nwSet) {
		this.nwSet = nwSet;
	}

	public String getResultKey() {
		return resultKey;
	}

	public void setResultKey(String resultKey) {
		this.resultKey = resultKey;
	}

	public IDatabase getDatabase() {
		return database;
	}

	public void setDatabase(IDatabase database) {
		this.database = database;
	}

	public ImpactMethodDescriptor getMethodDescriptor() {
		return methodDescriptor;
	}

	public void setMethodDescriptor(ImpactMethodDescriptor methodDescriptor) {
		this.methodDescriptor = methodDescriptor;
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
