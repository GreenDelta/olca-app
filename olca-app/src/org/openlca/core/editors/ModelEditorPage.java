/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.application.actions.OpenEditorAction;

/**
 * Abstract form page for model components
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class ModelEditorPage extends FormPage {

	private ScrolledForm form;

	public ModelEditorPage(ModelEditor editor, String id, String title) {
		super(editor, id, title);
	}

	protected void updateFormTitle() {
		if (form != null) {
			form.getForm().setText(getFormTitle());
		}
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		// configure form
		form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		toolkit.decorateFormHeading(form.getForm());
		form.setText(getFormTitle()); 

		IToolBarManager toolBar = form.getToolBarManager();
		RefreshAction action = new RefreshAction();
		toolBar.add(action);
		toolBar.update(true);

		Composite body = UI.formBody(getForm(), toolkit);

		createContents(body, toolkit);
		setData();
		initListeners();
		body.setFocus();
		form.reflow(true);
	}

	protected abstract void createContents(Composite body, FormToolkit toolkit);

	protected abstract String getFormTitle();

	protected void initListeners() {
	}

	protected abstract void setData();

	public ScrolledForm getForm() {
		return form;
	}

	private class RefreshAction extends Action {

		public RefreshAction() {
			setText(Messages.Reload);
			setImageDescriptor(ImageType.REFRESH_ICON.getDescriptor());
		}

		@Override
		public void run() {
			OpenEditorAction action = new OpenEditorAction();
			// action.setModelComponent(database,
			// ((ModelEditor) getEditor()).getModelComponent());
			action.run(true);
		}
	}

}
