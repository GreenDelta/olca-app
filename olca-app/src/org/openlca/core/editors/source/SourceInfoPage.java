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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorInfoPage;
import org.openlca.core.model.Source;
import org.openlca.ui.DataBinding;
import org.openlca.ui.UIFactory;

public class SourceInfoPage extends ModelEditorInfoPage {

	private Source source = null;
	private DataBinding binding = new DataBinding();
	private Composite composite;
	private FormToolkit toolkit;

	public SourceInfoPage(ModelEditor editor) {
		super(editor, "SourceInfoPage", Messages.Common_GeneralInformation,
				Messages.Common_GeneralInformation);
		this.source = (Source) editor.getModelComponent();
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		super.createContents(body, toolkit);
		this.toolkit = toolkit;
		Section section = UIFactory.createSection(body, toolkit,
				Messages.Sources_SourceInfoSectionLabel, true, false);
		composite = UIFactory.createSectionComposite(section, toolkit,
				UIFactory.createGridLayout(2));
		Text text = createText(Messages.Sources_Doi);
		binding.onString(source, "doi", text);
		text = createText(Messages.Sources_TextReference);
		binding.onString(source, "textReference", text);
		text = createText(Messages.Sources_Year);
		binding.onShort(source, "year", text);
	}

	private Text createText(String label) {
		Text text = UIFactory.createTextWithLabel(composite, toolkit, label,
				false);
		return text;
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Sources_FormText
				+ ": "
				+ (source != null ? source.getName() != null ? source.getName()
						: "" : "");
		return title;
	}

}
