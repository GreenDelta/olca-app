package org.openlca.app.editors.locations;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.app.rcp.html.HtmlFolder;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.openlca.util.BinUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LocationInfoPage extends ModelPage<Location> {

	String kml;
	boolean hasValidKml = true;

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ScrolledForm form;

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
		Section section = toolkit.createSection(body,
				ExpandableComposite.TITLE_BAR
						| ExpandableComposite.FOCUS_TITLE
						| ExpandableComposite.EXPANDED
						| ExpandableComposite.TWISTIE);
		UI.gridData(section, true, true);
		section.setText(M.KmlEditor);
		Composite comp = toolkit.createComposite(section);
		section.setClient(comp);
		UI.gridLayout(comp, 1);
		UI.gridData(comp, true, true);
		Browser browser = new Browser(comp, SWT.NONE);
		browser.setJavascriptEnabled(true);
		UI.gridData(browser, true, true).minimumHeight = 360;

		UI.bindFunction(browser, "onSave", (args) -> {
			String kml = null;
			if (args != null && args.length > 0 && args[0] != null) {
				kml = args[0].toString();
			}
			try {
				getModel().kmz = kml == null ? null
						: BinUtils.zip(kml.getBytes("utf-8"));
			} catch (Exception e) {
				throw new RuntimeException(
						"failed to convert KML to KMZ", e);
			}
			getEditor().setDirty(true);
			return null;
		});

		UI.onLoaded(browser, HtmlFolder.getUrl("kml_editor.html"), () -> {
			kml = KmlUtil.toKml(getModel().kmz);
			if (kml == null) {
				browser.execute("openEditor()");
				return;
			}
			kml = kml.replace("\r\n", "").replace("\n", "").replace("\r", "");
			try {
				browser.execute("openEditor('" + kml + "')");
			} catch (Exception e) {
				log.error("failed to set KML data", e);
			}
		});

	}
}
