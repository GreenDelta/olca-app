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
import org.openlca.core.model.results.ImpactResult;
import org.openlca.core.model.results.InventoryResult;

/**
 * Editor input for product system results.
 */
public class ResultEditorInput implements IEditorInput {

	private IDatabase database;
	private InventoryResult lciResult;
	private ImpactResult lciaResult;
	private String displayName;

	public ResultEditorInput(InventoryResult lciResult, IDatabase database) {
		this.database = database;
		this.lciResult = lciResult;
	}

	public void setImpactResult(ImpactResult lciaResult) {
		this.lciaResult = lciaResult;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	@Override
	public boolean exists() {
		return lciResult != null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	public IDatabase getDatabase() {
		return database;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public InventoryResult getLCIResult() {
		return lciResult;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	public ImpactResult getImpactResult() {
		return lciaResult;
	}

	@Override
	public String getToolTipText() {
		return "";
	}
}
