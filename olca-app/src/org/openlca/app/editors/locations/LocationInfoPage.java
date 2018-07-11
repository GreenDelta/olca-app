package org.openlca.app.editors.locations;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.processes.kml.KmlPrettifyFunction;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.web.WebEngine;

class LocationInfoPage extends ModelPage<Location> implements WebPage {

	String kml;
	boolean hasValidKml = true;

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private WebEngine webkit;
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
		Composite composite = UI.formSection(body, toolkit,
				M.AdditionalInformation, 3);
		text(composite, M.Code, "code");
		doubleText(composite, M.Longitude, "longitude");
		doubleText(composite, M.Latitude, "latitude");
	}

	private void createMapEditorArea(Composite body) {
		Section section = toolkit.createSection(body,
				ExpandableComposite.TITLE_BAR
						| ExpandableComposite.FOCUS_TITLE
						| ExpandableComposite.EXPANDED
						| ExpandableComposite.TWISTIE);
		UI.gridData(section, true, true);
		section.setText(M.KmlEditor);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		Actions.bind(section, new SaveKmlAction(), new ClearAction());
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, true);
		Control canvas = UI.createWebView(composite, this);
		UI.gridData(canvas, true, true).minimumHeight = 360;
	}

	@Override
	public String getUrl() {
		return HtmlView.KML_EDITOR.getUrl();
	}

	@Override
	public void onLoaded(WebEngine webkit) {
		this.webkit = webkit;
		UI.bindVar(webkit, "java", new JavaCallback());
		UI.bindVar(webkit, "prettifier", new KmlPrettifyFunction(b -> {
			hasValidKml = b;
		}));
		try {
			webkit.executeScript("setEmbedded()");
			webkit.executeScript("bridgeConsole()");
		} catch (Exception e) {
			log.error("failed to initialize KML editor", e);
		}
		updateKml();
	}

	void updateKml() {
		kml = KmlUtil.toKml(getModel().getKmz());
		if (kml == null)
			kml = "";
		kml = kml.replace("\r\n", "").replace("\n", "").replace("\r", "");
		try {
			webkit.executeScript("setKML('" + kml + "')");
		} catch (Exception e) {
			log.error("failed to set KML data", e);
		}
	}

	public class JavaCallback {

		public void kmlChanged(String data) {
			kml = data;
			try {
				hasValidKml = (Boolean) webkit.executeScript("isValidKml();");
				getEditor().setDirty(true);
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to call isValidKml", e);
			}
		}

		public void log(String message) {
			log.debug(message);
		}
	}

	private class ClearAction extends Action {

		private ClearAction() {
			super(M.ClearData);
			setImageDescriptor(Icon.DELETE.descriptor());
		}

		@Override
		public void run() {
			try {
				webkit.executeScript("onClear();");
				getEditor().setDirty(true);
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to call onClear", e);
			}
		}
	}

	private class SaveKmlAction extends Action {
		private SaveKmlAction() {
			super(M.Save);
			setImageDescriptor(Icon.SAVE.descriptor());
		}

		@Override
		public void run() {
			try {
				Object kmlObj = webkit.executeScript("getKML()");
				if (!(kmlObj instanceof String)) {
					log.debug("KML editor did not returned a string");
					kml = "";
				} else {
					kml = (String) kmlObj;
				}
				new KmlPrettifyFunction(b -> hasValidKml = b).prettifyKML(kml);
				getEditor().setDirty(true);
				IWorkbenchPage page = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				page.saveEditor(getEditor(), true);
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to save KM", e);
				Error.showBox("Failed to save KML: " + e.getMessage());
			}
		}
	}
}
