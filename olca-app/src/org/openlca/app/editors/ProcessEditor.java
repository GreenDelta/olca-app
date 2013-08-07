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

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.core.editors.IEditor;
import org.openlca.core.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.util.Calendar;

public class ProcessEditor extends ModelEditor<Process> implements IEditor {

	public static String ID = "editors.process";
	private Logger log = LoggerFactory.getLogger(getClass());

	public ProcessEditor() {
		super(Process.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProcessInfoPage(this));
			addPage(new ProcessExchangePage(this));
			addPage(new ProcessAdminInfoPage(this));
			addPage(new ProcessModelingPage(this));
			addPage(new ProcessParameterPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		getModel().getDocumentation().setLastChange(
				Calendar.getInstance().getTime());
		super.doSave(monitor);
	}

}
