/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.ModelEditorInput;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens editors for a set of model components
 * 
 * TODO: use App.openEditor in this class
 */
public class OpenEditorAction extends Action {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public static final String ID = "open.action";
	public static final String TEXT = Messages.OpenEditorAction_Text;

	private List<Item> items = new ArrayList<>();
	private boolean refresh = false;
	private Map<Class<?>, String> editorIds = new HashMap<>();
	private IWorkbenchPage page;

	public OpenEditorAction() {
		setId(ID);
		setText(TEXT);
	}

	@Override
	public void run() {
		if (items == null || items.isEmpty())
			return;
		page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage();
		for (Item item : items) {
			if (!item.canOpen())
				continue;
			log.trace("open {}", item);
			ModelEditorInput input = item.toInput();
			IEditorPart editor = page.findEditor(input);
			if (editor == null)
				open(item, null);
			else if (!refresh)
				editor.getEditorSite().getPage().activate(editor);
			else {
				editor.getEditorSite().getPage().closeEditor(editor, true);
				open(item, getPageId(editor));
			}
		}
	}

	private void open(Item item, String pageId) {
		try {
			IEditorPart editorPart = page.openEditor(item.toInput(),
					item.editorId, true);
			if (pageId != null && editorPart != null) {
				((FormEditor) editorPart).setActivePage(pageId);
			}
		} catch (final PartInitException e) {
			log.error("Open editor failed", e);
		}
	}

	private String getPageId(IEditorPart editor) {
		if (editor instanceof FormEditor) {
			IFormPage pageInstance = ((FormEditor) editor)
					.getActivePageInstance();
			if (pageInstance != null)
				return pageInstance.getId();
		}
		return null;
	}

	/**
	 * Runs the action with a refresh flag: if true an already opened editor
	 * will be closed and reopened.
	 */
	public void run(boolean refresh) {
		this.refresh = refresh;
		run();
		this.refresh = refresh;
	}

	/** Set the model to be opened. */
	public void setModelComponent(IDatabase database,
			IModelComponent modelComponent) {
		items.clear();
		items.add(new Item(modelComponent, database));
	}

	/** Set the models to be opened. */
	public void setModelComponents(IDatabase database,
			IModelComponent[] modelComponents) {
		items.clear();
		for (IModelComponent comp : modelComponents)
			items.add(new Item(comp, database));
	}

	/** Set the models to be opened. */
	public void setModelComponents(IDatabase[] databases,
			IModelComponent[] modelComponents) {
		items.clear();
		for (int i = 0; i < modelComponents.length; i++)
			items.add(new Item(modelComponents[i], databases[i]));
	}

	/** Defines an item that is set to be opened. */
	private class Item {
		IModelComponent component;
		IDatabase database;
		String editorId;

		public Item(IModelComponent model, IDatabase db) {
			this.component = model;
			this.database = db;
			setEditorId();
		}

		private void setEditorId() {
			if (component == null)
				return;
			editorId = editorIds.get(component.getClass());
			if (editorId == null) {
				editorId = findEditorId();
				editorIds.put(component.getClass(), editorId);
			}
		}

		private String findEditorId() {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry
					.getConfigurationElementsFor("org.openlca.core.application.editors");
			String className = component.getClass().getCanonicalName();
			for (IConfigurationElement elem : elements) {
				String clazz = elem.getAttribute("componentClass");
				if (clazz != null && clazz.equals(className))
					return elem.getAttribute("editorID");
			}
			return null;
		}

		public boolean canOpen() {
			return component != null && database != null && editorId != null;
		}

		public ModelEditorInput toInput() {
			return new ModelEditorInput(Descriptors.toDescriptor(component),
					database);
		}

		@Override
		public String toString() {
			return component + " @ " + database;
		}

	}

}
