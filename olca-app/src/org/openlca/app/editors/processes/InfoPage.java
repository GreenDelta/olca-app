package org.openlca.app.editors.processes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.processes.kml.EditorHandler;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.app.editors.processes.kml.MapEditor;
import org.openlca.app.editors.processes.kml.TextEditor;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ExchangeViewer;
import org.openlca.app.viewers.combo.LocationViewer;
import org.openlca.core.model.Process;

class InfoPage extends ModelPage<Process> {

	private FormToolkit toolkit;
	private ImageHyperlink kmlLink;
	private ScrolledForm form;

	InfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm, Messages.Process + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
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
		Composite composite = UI.formSection(body, toolkit,
				Messages.TechnologyInfoSectionLabel);
		createMultiText(Messages.Description, "documentation.technology",
				composite);
	}

	private void createTimeSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.TimeInfoSectionLabel);
		createDate(Messages.StartDate, "documentation.validFrom", composite);
		createDate(Messages.EndDate, "documentation.validUntil", composite);
		createMultiText(Messages.Description, "documentation.time", composite);
	}

	private void createGeographySection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.GeographyInfoSectionLabel);
		toolkit.createLabel(composite, Messages.Location);
		LocationViewer locationViewer = new LocationViewer(composite);
		locationViewer.setNullable(true);
		locationViewer.setInput(Database.get());
		getBinding().on(getModel(), "location", locationViewer);
		createKmlSection(composite);
		createMultiText(Messages.Description, "documentation.geography",
				composite);
	}

	private void createKmlSection(Composite parent) {
		UI.formLabel(parent, "KML");
		Composite composite = toolkit.createComposite(parent);
		UI.gridData(composite, true, true);
		GridLayout layout = UI.gridLayout(composite, 2);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		kmlLink = toolkit.createImageHyperlink(composite, SWT.TOP);
		kmlLink.addHyperlinkListener(new MapEditorDispatch());
		UI.gridData(kmlLink, true, false).horizontalSpan = 2;
		kmlLink.setText(KmlUtil.getDisplayText(getModel().getKmz()));
		Button mapButton = toolkit.createButton(composite, "Map editor",
				SWT.NONE);
		mapButton.addSelectionListener(new MapEditorDispatch());
		mapButton.setImage(ImageType.LCIA_ICON.get());
		Button textButton = toolkit.createButton(composite, "Text editor",
				SWT.NONE);
		textButton.setImage(ImageType.FILE_MARKUP_SMALL.get());
		textButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TextEditor.open(KmlUtil.toKml(getModel().getKmz()),
						new MapEditorDispatch());
			}
		});
	}

	private class MapEditorDispatch extends HyperlinkAdapter implements
			SelectionListener, EditorHandler {

		public void widgetDefaultSelected(SelectionEvent e) {
			openEditor();
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			openEditor();
		}

		@Override
		public void linkActivated(HyperlinkEvent e) {
			openEditor();
		}

		@Override
		public void contentSaved(String kml) {
			Process process = getModel();
			process.setKmz(KmlUtil.toKmz(kml));
			getEditor().setDirty(true);
			kmlLink.setText(KmlUtil.getDisplayText(process.getKmz()));
		}

		private void openEditor() {
			MapEditor.open(KmlUtil.toKml(getModel().getKmz()), this);
		}
	}

}
