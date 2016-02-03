package org.openlca.app.editors.processes;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.Event;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.processes.kml.EditorHandler;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.app.editors.processes.kml.MapEditor;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ExchangeViewer;
import org.openlca.app.viewers.combo.LocationViewer;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.util.KeyGen;

import com.google.common.eventbus.Subscribe;

class InfoPage extends ModelPage<Process> {

	private ProcessEditor editor;
	private FormToolkit toolkit;
	private ImageHyperlink kmlLink;
	private ScrolledForm form;
	private ExchangeViewer quanRefViewer;
	private LocationViewer locationViewer;

	InfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", M.GeneralInformation);
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
		form = UI.formHeader(managedForm);
		updateFormTitle();
		if (FeatureFlag.SHOW_REFRESH_BUTTONS.isEnabled())
			Editors.addRefresh(form, editor);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createCheckBox(M.InfrastructureProcess, "infrastructureProcess",
				infoSection.getContainer());
		createSystemButton(infoSection.getContainer());
		createQuanRefSection(body);
		createTimeSection(body);
		createGeographySection(body);
		createTechnologySection(body);
		body.setFocus();
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.Process + ": " + getModel().getName());
	}

	private void createSystemButton(Composite container) {
		toolkit.createLabel(container, "");
		Button button = toolkit.createButton(container,
				M.CreateProductSystem, SWT.NONE);
		button.setImage(Images.get(ModelType.PRODUCT_SYSTEM, Overlay.NEW));
		Controls.onSelect(button, (e) -> {
			SystemCreation.run(getModel());
		});
	}

	private void createQuanRefSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				M.QuantitativeReference);
		UI.formLabel(composite, toolkit, M.QuantitativeReference);
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
		Composite composite = UI
				.formSection(body, toolkit, M.Technology);
		createMultiText(M.Description, "documentation.technology",
				composite);
	}

	private void createTimeSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit, M.Time);
		createDate(M.StartDate, "documentation.validFrom", composite);
		createDate(M.EndDate, "documentation.validUntil", composite);
		createMultiText(M.Description, "documentation.time", composite);
	}

	private void createGeographySection(Composite body) {
		Composite composite = UI.formSection(body, toolkit, M.Geography);
		toolkit.createLabel(composite, M.Location);
		locationViewer = new LocationViewer(composite);
		locationViewer.setNullable(true);
		locationViewer.setInput(Database.get());
		getBinding().onModel(() -> getModel(), "location", locationViewer);
		locationViewer.addSelectionChangedListener((s) -> kmlLink
				.setText(getKmlDisplayText()));
		createKmlSection(composite);
		createMultiText(M.Description, "documentation.geography",
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
	}

	private String getKmlDisplayText() {
		Process process = getModel();
		if (process.getLocation() != null)
			if (process.getLocation().getKmz() != null)
				return KmlUtil.getDisplayText(process.getLocation().getKmz());
		return "none";
	}

	private void openMap(EditorHandler handler) {
		String name = getName();
		String kml = getKml();
		if (kml != null)
			// prepare the KML for the map so that no syntax errors are thrown
			kml = kml.trim().replace("\n", "").replace("\r", "");
		MapEditor.open(name, kml, handler);
	}

	private String getKml() {
		Process process = getModel();
		if (process.getLocation() != null)
			if (process.getLocation().getKmz() != null)
				return KmlUtil.toKml(process.getLocation().getKmz());
		return null;
	}

	private String getName() {
		if (getModel().getLocation() == null)
			return "";
		return getModel().getLocation().getName();
	}

	private class MapEditorDispatch extends HyperlinkAdapter implements
			SelectionListener, EditorHandler {

		private LocationDao locationDao = new LocationDao(Database.get());

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
		public boolean contentSaved(String kml) {
			Location location = createLocation(kml);
			if (location == null) // user aborted
				return false;
			locationViewer.setInput(Database.get());
			locationViewer.select(location);
			kmlLink.setText(getKmlDisplayText());
			return true;
		}

		private Location createLocation(String kml) {
			Location location = new Location();
			String[] nameAndCode = promptForNameAndCode();
			if (nameAndCode == null) // user aborted dialog
				return null;
			location.setName(nameAndCode[0]);
			location.setCode(nameAndCode[1]);
			location.setRefId(KeyGen.get(nameAndCode[1]));
			if (kml != null)
				location.setKmz(KmlUtil.toKmz(kml));
			else
				location.setKmz(null);
			return locationDao.insert(location);
		}

		@Override
		public void openModel() {
			if (getModel().getLocation() == null)
				return;
			App.openEditor(getModel().getLocation());
		}

		private String[] promptForNameAndCode() {
			NameAndCodeDialog dialog = new NameAndCodeDialog();
			if (dialog.open() == Dialog.CANCEL)
				return null;
			return new String[] { dialog.name, dialog.code };
		}

		private class NameAndCodeDialog extends Dialog {

			private String name;
			private String code;

			private NameAndCodeDialog() {
				super(UI.shell());
			}

			@Override
			protected Control createDialogArea(Composite parent) {
				Composite body = new Composite(parent, SWT.NONE);
				UI.gridLayout(body, 1);
				UI.gridData(body, true, true);
				UI.formLabel(body,
						M.EnterLocationNameAndCode);
				Composite container = UI.formComposite(body);
				UI.gridData(container, true, true);
				final Text nameText = UI.formText(container, M.Name);
				nameText.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(ModifyEvent e) {
						name = nameText.getText();
						updateButtons();
					}
				});
				final Text codeText = UI.formText(container, M.Code);
				codeText.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(ModifyEvent e) {
						code = codeText.getText();
						updateButtons();
					}
				});
				return body;
			}

			private void updateButtons() {
				getButton(IDialogConstants.OK_ID).setEnabled(isPageComplete());
			}

			private boolean isPageComplete() {
				return name != null && !name.isEmpty();
			}

			@Override
			protected Control createButtonBar(Composite parent) {
				Control buttonBar = super.createButtonBar(parent);
				updateButtons();
				return buttonBar;
			}

		}

	}

}
