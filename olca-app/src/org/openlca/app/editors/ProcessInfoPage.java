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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ExchangeViewer;
import org.openlca.app.viewers.combo.LocationViewer;
import org.openlca.core.model.Process;

class ProcessInfoPage extends ModelPage<Process> {

	private FormToolkit toolkit;

	ProcessInfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Process + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getModel(), getBinding());
		infoSection.render(body, toolkit);
		createAdditionalInfo(infoSection, body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(InfoSection infoSection, Composite body) {
		createCheckBox(Messages.InfrastructureProcess, "infrastructureProcess",
				infoSection.getContainer());

		new Label(infoSection.getContainer(), SWT.NONE)
				.setText(Messages.QuantitativeReference);
		ExchangeViewer referenceViewer = new ExchangeViewer(
				infoSection.getContainer(), ExchangeViewer.OUTPUTS,
				ExchangeViewer.PRODUCTS);
		referenceViewer.setInput(getModel());
		getBinding().on(getModel(), "quantitativeReference", referenceViewer);

		Composite timeComposite = UI.formSection(body, toolkit,
				Messages.TimeInfoSectionLabel);
		createDate(Messages.StartDate, "documentation.validFrom", timeComposite);
		createDate(Messages.EndDate, "documentation.validUntil", timeComposite);
		createMultiText(Messages.Comment, "documentation.time", timeComposite);

		Composite geographyComposite = UI.formSection(body, toolkit,
				Messages.GeographyInfoSectionLabel);
		toolkit.createLabel(geographyComposite, Messages.Location);
		LocationViewer locationViewer = new LocationViewer(geographyComposite);
		locationViewer.setNullable(true);
		locationViewer.setInput(Database.get());
		getBinding().on(getModel(), "location", locationViewer);
		createMultiText(Messages.Comment, "documentation.geography",
				geographyComposite);

		Composite technologyComposite = UI.formSection(body, toolkit,
				Messages.TechnologyInfoSectionLabel);
		createMultiText(Messages.Comment, "documentation.technology",
				technologyComposite);
	}
}
