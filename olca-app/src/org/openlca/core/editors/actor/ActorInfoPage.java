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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.InfoSection;
import org.openlca.core.model.Actor;
import org.openlca.ui.DataBinding;
import org.openlca.ui.UI;

public class ActorInfoPage extends FormPage {

	private Actor actor;
	private FormToolkit toolkit;
	private DataBinding binding;

	public ActorInfoPage(ActorEditor editor) {
		super(editor, "ActorInfoPage", Messages.Common_GeneralInformation);
		this.actor = editor.getActor();
		this.binding = new DataBinding(editor);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Actor + ": "
				+ actor.getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(actor, binding);
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.Common_AdditionalInfo);
		createText(Messages.Address, "address", composite);
		createText(Messages.City, "city", composite);
		createText(Messages.Country, "country", composite);
		createText(Messages.EMail, "email", composite);
		createText(Messages.Telefax, "telefax", composite);
		createText(Messages.Telephone, "telephone", composite);
		createText(Messages.WebSite, "webSite", composite);
		createText(Messages.ZipCode, "zipCode", composite);
	}

	private void createText(String label, String property, Composite parent) {
		Text text = UI.formText(parent, toolkit, label);
		binding.onString(actor, property, text);
	}

}
