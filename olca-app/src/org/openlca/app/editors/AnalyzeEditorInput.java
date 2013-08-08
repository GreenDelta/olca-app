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
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;

/**
 * Input for the analysis editor.
 */
public class AnalyzeEditorInput implements IEditorInput {

	private String resultKey;
	private Long nwSetId;
	private Long methodId;
	private CalculationType type;

	public String getResultKey() {
		return resultKey;
	}

	public void setResultKey(String resultKey) {
		this.resultKey = resultKey;
	}

	public CalculationType getType() {
		return type;
	}

	public void setType(CalculationType type) {
		this.type = type;
	}

	/**
	 * Cache the nw set since it is loaded from the database every time this
	 * method gets called
	 */
	public NormalizationWeightingSet getNwSet() {
		if (nwSetId == null)
			return null;
		if (methodId == null)
			return null;
		return new BaseDao<>(NormalizationWeightingSet.class, Database.get())
				.getForId(nwSetId);
	}

	public void setNwSetId(Long nwSetId) {
		this.nwSetId = nwSetId;
	}

	public ImpactMethodDescriptor getMethodDescriptor() {
		if (methodId == null)
			return null;
		return Database.getCache().getImpactMethodDescriptor(methodId);
	}

	public void setMethodId(Long methodId) {
		this.methodId = methodId;
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
