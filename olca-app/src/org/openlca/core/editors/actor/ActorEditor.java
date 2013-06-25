/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.actor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.views.ModelEditorInput;
import org.openlca.core.database.ActorDao;
import org.openlca.core.editors.IEditor;
import org.openlca.core.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorEditor extends FormEditor implements IEditor {

	private Logger log = LoggerFactory.getLogger(getClass());
	public static String ID = "ActorEditor";
	private Actor actor;
	private ActorDao dao;
	private boolean dirty;

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		log.trace("open actor editor {}", input);
		setPartName(input.getName());
		try {
			dao = new ActorDao(Database.getEntityFactory());
			ModelEditorInput i = (ModelEditorInput) input;
			actor = dao.getForId(i.getDescriptor().getId());
		} catch (Exception e) {
			log.error("failed to load actor from editor input", e);
		}
	}

	public Actor getActor() {
		return actor;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ActorInfoPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			monitor.beginTask("Save actor...", IProgressMonitor.UNKNOWN);
			dao.update(actor);
			dirty = false;
			editorDirtyStateChanged();
			monitor.done();
		} catch (Exception e) {
			log.error("failed to update actor");
		}
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void setDirty(boolean b) {
		if (dirty != b) {
			dirty = b;
			editorDirtyStateChanged();
		}
	}

}
