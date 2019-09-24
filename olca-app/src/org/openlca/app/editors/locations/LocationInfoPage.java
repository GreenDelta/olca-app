package org.openlca.app.editors.locations;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.FileType;
import org.openlca.app.util.KmlUtil;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

class LocationInfoPage extends ModelPage<Location> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ScrolledForm form;
	private Browser browser;

	private final int KML_MAP_TOOL = 0;
	private final int KML_TEXT_TOOL = 1;
	private int kmlTool = KML_MAP_TOOL;

	LocationInfoPage(LocationEditor editor) {
		super(editor, "LocationInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		createMapEditorArea(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(Composite body) {
		Composite comp = UI.formSection(body, toolkit,
				M.AdditionalInformation, 3);
		text(comp, M.Code, "code");
		doubleText(comp, M.Longitude, "longitude");
		doubleText(comp, M.Latitude, "latitude");
	}

	private void createMapEditorArea(Composite body) {
		Section section = UI.section(body, toolkit, M.KmlEditor);
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, toolkit);
		UI.gridLayout(comp, 1);
		UI.gridData(comp, true, true);
		browser = new Browser(comp, SWT.NONE);
		browser.setJavascriptEnabled(true);
		UI.gridData(browser, true, true).minimumHeight = 360;

		UI.onLoaded(browser, HtmlFolder.getUrl("kml_editor.html"), () -> {
			openKmlTool(KmlUtil.toKml(getModel().kmz), KML_MAP_TOOL);
		});

		Action showMap = Actions.create(M.Map,
				Icon.MAP.descriptor(),
				() -> openKmlTool(KmlUtil.toKml(getModel().kmz), KML_MAP_TOOL));
		Action showText = Actions.create(M.Text,
				Images.descriptor(FileType.MARKUP),
				() -> openKmlTool(KmlUtil.toKml(getModel().kmz), KML_TEXT_TOOL));
		Action clear = Actions.onRemove(this::clearKml);
		Action save = Actions.onSave(this::saveKml);
		Actions.bind(section, showMap, showText, clear, save);
	}

	private void openKmlTool(String kml, int tool) {
		this.kmlTool = tool;
		String fn = tool == KML_MAP_TOOL
				? "openMap"
				: "openText";
		try {
			if (Strings.isNullOrEmpty(kml)) {
				browser.execute(fn + "()");
				return;
			}
			browser.execute(fn + "('"
					+ kml.replaceAll("\\R", " ") + "')");
		} catch (Exception e) {
			log.error("failed to set KML data via " + fn, e);
		}
	}

	private void clearKml() {
		Location loc = getModel();
		loc.kmz = null;
		getEditor().setDirty(true);
		openKmlTool(null, kmlTool);
	}

	private void saveKml() {
		try {
			Object obj = browser.evaluate("return getKml()");
			String kml = obj == null ? null : obj.toString();
			// TODO: validate KML
			byte[] kmz = KmlUtil.toKmz(kml);
			getModel().kmz = kmz;
			getEditor().setDirty(true);
		} catch (Exception e) {
			log.error("failed to get KML from browser", e);
		}
	}

	void refreshKmlView() {
		openKmlTool(KmlUtil.toKml(getModel().kmz), kmlTool);
	}
}
