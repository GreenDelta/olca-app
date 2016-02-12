package org.openlca.app.editors.processes.kml;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.M;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapEditor implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Shell shell;
	private final Browser browser;
	private String kml;
	private EditorHandler handler;

	public static void open(String name, String kml, EditorHandler saveHandler) {
		MapEditor editor = new MapEditor(name, kml, saveHandler);
		editor.openShell();
	}

	private MapEditor(String name, String kml, EditorHandler saveHandler) {
		this.kml = kml;
		this.handler = saveHandler;
		Shell parent = UI.shell();
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(new FillLayout());
		shell.setText(M.KmlEditor);
		browser = UI.createBrowser(shell, this);
		Point parentSize = parent.getSize();
		shell.setSize((int) (parentSize.x * 0.85), (int) (parentSize.y * 0.85));
		UI.center(parent, shell);
	}

	private void openShell() {
		shell.open();
	}

	public void close() {
		shell.close();
	}

	@Override
	public String getUrl() {
		return HtmlView.KML_EDITOR.getUrl();
	}

	@Override
	public void onLoaded() {
		new LocationOpenFunction();
		new LocationSaveFunction();
		new KmlPrettifyFunction(browser, null);
		if (kml == null)
			kml = "";
		try {
			browser.evaluate("setKML('" + kml + "')");
			browser.evaluate("setUpdateButtonVisible(" + handler.hasModel() + ")");
		} catch (Exception e) {
			log.error("failed to set KML data", e);
		}
	}

	private class LocationSaveFunction extends BrowserFunction {

		public LocationSaveFunction() {
			super(browser, "doSave");
		}

		@Override
		public Object function(Object[] args) {
			if (handler == null)
				return null;
			String kml = getArg(args, 0);
			boolean isValid = (Boolean) browser.evaluate("return isValidKml()");
			if (!isValid) {
				Info.showBox("The kml you provided is not valid, please check your input");
				return null;
			}
			if (handler.contentSaved(kml)) {
				handler.openModel();
				close();
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		private <T> T getArg(Object[] args, int index) {
			if (args.length <= index)
				return null;
			return (T) args[index];
		}

	}

	private class LocationOpenFunction extends BrowserFunction {

		public LocationOpenFunction() {
			super(browser, "doOpenEditor");
		}

		@Override
		public Object function(Object[] args) {
			if (handler == null)
				return null;
			handler.openModel();
			close();
			return null;
		}

	}

}
