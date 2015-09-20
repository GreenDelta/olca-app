package org.openlca.app.editors.locations;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.app.editors.processes.kml.MapEditor.KmlPrettifyFunction;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationInfoPage extends ModelPage<Location> implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private Browser browser;
	private String kml;

	LocationInfoPage(LocationEditor editor) {
		super(editor, "LocationInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Location + ": " + getModel().getName());
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
		Composite composite = UI.formSection(body, toolkit, Messages.AdditionalInformation);
		createText(Messages.Code, "code", composite);
		createDoubleText(Messages.Longitude, "longitude", composite);
		createDoubleText(Messages.Latitude, "latitude", composite);
	}

	private void createMapEditorArea(Composite body) {
		Section section = toolkit.createSection(body, ExpandableComposite.TITLE_BAR | ExpandableComposite.FOCUS_TITLE
				| ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
		UI.gridData(section, true, true);
		section.setText(Messages.KmlEditor);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, true);
		browser = UI.createBrowser(composite, this);
		UI.gridData(browser, true, true);
	}

	@Override
	public String getUrl() {
		return HtmlView.KML_EDITOR.getUrl();
	}

	String getKml() {
		return kml;
	}

	@Override
	public void onLoaded() {
		new KmlChangedFunction(browser);
		new KmlPrettifyFunction(browser);
		kml = KmlUtil.toKml(getModel().getKmz());
		if (kml == null)
			kml = "";
		try {
			browser.evaluate("setKML('" + kml + "')");
			browser.evaluate("setEmbedded()");
		} catch (Exception e) {
			log.error("failed to set KML data", e);
		}
	}

	private class KmlChangedFunction extends BrowserFunction {

		private KmlChangedFunction(Browser browser) {
			super(browser, "kmlChanged");
		}

		@Override
		public Object function(Object[] arguments) {
			kml = getArg(arguments, 0);
			getEditor().setDirty(true);
			return null;
		}

		@SuppressWarnings("unchecked")
		private <T> T getArg(Object[] args, int index) {
			if (args.length <= index)
				return null;
			return (T) args[index];
		}
	}

}
