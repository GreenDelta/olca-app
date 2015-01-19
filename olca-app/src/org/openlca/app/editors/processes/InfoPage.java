package org.openlca.app.editors.processes;

import org.eclipse.swt.SWT;
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
import org.openlca.app.Event;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.processes.kml.EditorHandler;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.app.editors.processes.kml.MapEditor;
import org.openlca.app.editors.processes.kml.TextEditor;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ExchangeViewer;
import org.openlca.app.viewers.combo.LocationViewer;
import org.openlca.core.model.Process;

import com.google.common.eventbus.Subscribe;

class InfoPage extends ModelPage<Process> {

	private ProcessEditor editor;
	private FormToolkit toolkit;
	private ImageHyperlink kmlLink;
	private ScrolledForm form;
	private ExchangeViewer quanRefViewer;

	InfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", Messages.GeneralInformation);
		this.editor = editor;
		editor.getEventBus().register(this);
	}

	@Subscribe
	public void handleExchangesChange(Event event) {
		if (!event.match(editor.EXCHANGES_CHANGED))
			return;
		quanRefViewer.setInput(getModel());
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm, Messages.Process + ": "
				+ getModel().getName());
		if (FeatureFlag.SHOW_REFRESH_BUTTONS.isEnabled())
			Editors.addRefresh(form, editor);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createCheckBox(Messages.InfrastructureProcess, "infrastructureProcess",
				infoSection.getContainer());
		createSystemButton(infoSection.getContainer());
		createQuanRefSection(body);
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
		Controls.onSelect(button, (e) -> {
			SystemCreation.run(getModel());
		});
	}

	private void createQuanRefSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.QuantitativeReference);
		UI.formLabel(composite, toolkit, Messages.QuantitativeReference);
		quanRefViewer = new ExchangeViewer(composite, ExchangeViewer.OUTPUTS,
				ExchangeViewer.PRODUCTS);
		quanRefViewer.setInput(getModel());
		getBinding().onModel(() -> getModel(), "quantitativeReference",
				quanRefViewer);
		editor.onSaved(() -> {
			quanRefViewer.setInput(getModel());
			quanRefViewer.select(getModel().getQuantitativeReference());
		});
	}

	private void createTechnologySection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.Technology);
		createMultiText(Messages.Description, "documentation.technology",
				composite);
	}

	private void createTimeSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.Time);
		createDate(Messages.StartDate, "documentation.validFrom", composite);
		createDate(Messages.EndDate, "documentation.validUntil", composite);
		createMultiText(Messages.Description, "documentation.time", composite);
	}

	private void createGeographySection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.Geography);
		toolkit.createLabel(composite, Messages.Location);
		LocationViewer viewer = new LocationViewer(composite);
		viewer.setNullable(true);
		viewer.setInput(Database.get());
		getBinding().onModel(() -> getModel(), "location", viewer);
		viewer.addSelectionChangedListener((s) -> kmlLink
				.setText(getKmlDisplayText()));
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
		kmlLink.setText(getKmlDisplayText());
		Button mapButton = toolkit.createButton(composite, "Map editor",
				SWT.NONE);
		mapButton.addSelectionListener(new MapEditorDispatch());
		mapButton.setImage(ImageType.LCIA_ICON.get());
		Button textButton = toolkit.createButton(composite, "Text editor",
				SWT.NONE);
		textButton.setImage(ImageType.FILE_MARKUP_SMALL.get());
		Controls.onSelect(textButton, (e) -> {
			TextEditor.open(getKml(), new MapEditorDispatch());
		});
	}

	private String getKmlDisplayText() {
		Process process = getModel();
		if (process.getKmz() != null)
			return KmlUtil.getDisplayText(process.getKmz()) + " (process)";
		if (process.getLocation() != null)
			return KmlUtil.getDisplayText(process.getLocation().getKmz())
					+ " (location)";
		else
			return "none";
	}

	private void openMap(EditorHandler handler) {
		String kml = getKml();
		if (kml != null) {
			// prepare the KML for the map so that no syntax errors are thrown
			kml = kml.trim().replace("\n", "").replace("\r", "");
		}
		MapEditor.open(kml, handler);

	}

	private String getKml() {
		Process process = getModel();
		if (process.getKmz() != null)
			return KmlUtil.toKml(process.getKmz());
		if (process.getLocation() != null)
			return KmlUtil.toKml(process.getLocation().getKmz());
		else
			return null;
	}

	private class MapEditorDispatch extends HyperlinkAdapter implements
			SelectionListener, EditorHandler {

		public void widgetDefaultSelected(SelectionEvent e) {
			openMap(this);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			openMap(this);
		}

		@Override
		public void linkActivated(HyperlinkEvent e) {
			openMap(this);
		}

		@Override
		public void contentSaved(String kml) {
			Process process = getModel();
			if (kml == null || kml.trim().isEmpty())
				process.setKmz(null);
			else
				process.setKmz(KmlUtil.toKmz(kml));
			getEditor().setDirty(true);
			kmlLink.setText(getKmlDisplayText());
		}
	}

}
