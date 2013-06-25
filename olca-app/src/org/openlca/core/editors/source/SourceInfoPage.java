/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.editors.source;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.InfoSection;
import org.openlca.core.model.Source;
import org.openlca.ui.DataBinding;
import org.openlca.ui.UI;

public class SourceInfoPage extends FormPage {

	private Source source;
	private DataBinding binding;
	private FormToolkit toolkit;

	public SourceInfoPage(SourceEditor editor) {
		super(editor, "SourceInfoPage", Messages.Common_GeneralInformation);
		this.source = editor.getSource();
		this.binding = new DataBinding(editor);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.Sources_FormText + ": " + source.getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(source, binding);
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	protected void createAdditionalInfo(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.Sources_SourceInfoSectionLabel);
		Text text = UI.formText(composite, toolkit, Messages.Sources_Doi);
		binding.onString(source, "doi", text);
		text = UI.formText(composite, toolkit, Messages.Sources_TextReference);
		binding.onString(source, "textReference", text);
		text = UI.formText(composite, toolkit, Messages.Sources_Year);
		binding.onShort(source, "year", text);
	}

}
