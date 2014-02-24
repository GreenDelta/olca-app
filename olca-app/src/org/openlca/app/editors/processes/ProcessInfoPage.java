package org.openlca.app.editors.processes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.resources.ImageType;
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
		createCheckBox(Messages.InfrastructureProcess, "infrastructureProcess",
				infoSection.getContainer());
		createSystemButton(infoSection.getContainer());
		createQuantitativeReferenceSection(body);
		createTimeSection(body);
		createGeographySection(body);
		createTechnologySection(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createSystemButton(Composite container) {
		toolkit.createLabel(container, "");
		Button button = toolkit.createButton(container,
				Messages.CreateProductSystem, SWT.NONE);
		button.setImage(ImageType.PRODUCT_SYSTEM_ICON_NEW.get());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SystemCreation.run(getModel());
			}
		});
	}

	private void createQuantitativeReferenceSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.QuantitativeReference);
		UI.formLabel(composite, toolkit, Messages.QuantitativeReference);
		ExchangeViewer referenceViewer = new ExchangeViewer(composite,
				ExchangeViewer.OUTPUTS, ExchangeViewer.PRODUCTS);
		referenceViewer.setInput(getModel());
		getBinding().on(getModel(), "quantitativeReference", referenceViewer);
	}

	private void createTechnologySection(Composite body) {
		Composite technologyComposite = UI.formSection(body, toolkit,
				Messages.TechnologyInfoSectionLabel);
		createMultiText(Messages.Comment, "documentation.technology",
				technologyComposite);
	}

	private void createTimeSection(Composite body) {
		Composite timeComposite = UI.formSection(body, toolkit,
				Messages.TimeInfoSectionLabel);
		createDate(Messages.StartDate, "documentation.validFrom", timeComposite);
		createDate(Messages.EndDate, "documentation.validUntil", timeComposite);
		createMultiText(Messages.Comment, "documentation.time", timeComposite);
	}

	private void createGeographySection(Composite body) {
		Composite geographyComposite = UI.formSection(body, toolkit,
				Messages.GeographyInfoSectionLabel);
		toolkit.createLabel(geographyComposite, Messages.Location);
		LocationViewer locationViewer = new LocationViewer(geographyComposite);
		locationViewer.setNullable(true);
		locationViewer.setInput(Database.get());
		getBinding().on(getModel(), "location", locationViewer);
		createMultiText(Messages.Comment, "documentation.geography",
				geographyComposite);
	}
}
