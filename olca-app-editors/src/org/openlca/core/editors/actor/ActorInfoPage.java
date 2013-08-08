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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorInfoPage;
import org.openlca.core.model.Actor;
import org.openlca.ui.DataBinding;
import org.openlca.ui.UIFactory;

public class ActorInfoPage extends ModelEditorInfoPage {

	private Actor actor;
	private FormToolkit toolkit;
	private Composite composite;
	private DataBinding binding = new DataBinding();

	public ActorInfoPage(final ModelEditor editor) {
		super(editor, "ActorInfoPage", Messages.Common_GeneralInformation,
				Messages.Common_GeneralInformation);
		this.actor = (Actor) editor.getModelComponent();
	}

	@Override
	protected void createContents(Composite body, FormToolkit toolkit) {
		super.createContents(body, toolkit);
		this.toolkit = toolkit;
		getSite().setSelectionProvider(this);
		Section section = UIFactory.createSection(body, toolkit,
				Messages.Common_AdditionalInfo, true, false);
		composite = UIFactory.createSectionComposite(section, toolkit,
				UIFactory.createGridLayout(2));
		createTextFields();
	}

	private void createTextFields() {
		createText(Messages.Address, "address");
		createText(Messages.City, "city");
		createText(Messages.Country, "country");
		createText(Messages.EMail, "EMail");
		createText(Messages.Telefax, "telefax");
		createText(Messages.Telephone, "telephone");
		createText(Messages.WebSite, "webSite");
		createText(Messages.ZipCode, "zipCode");
	}

	private void createText(String label, String property) {
		Text text = UIFactory.createTextWithLabel(composite, toolkit, label,
				false);
		binding.onString(actor, property, text);
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Actor
				+ ": "
				+ (actor != null ? actor.getName() != null ? actor.getName()
						: "" : "");
		return title;
	}

}
