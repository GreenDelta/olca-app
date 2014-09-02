package org.openlca.app.editors.processes.kml;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.Messages;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapEditor implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Shell shell;
	private final Browser browser;
	private String kml;
	private EditorHandler handler;

	public static void open(String kml, EditorHandler handler) {
		MapEditor editor = new MapEditor(kml, handler);
		editor.openShell();
	}

	private MapEditor(String kml, EditorHandler handler) {
		this.kml = kml;
		this.handler = handler;
		Shell parent = UI.shell();
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(new FillLayout());
		shell.setText(Messages.KmlEditor);
		browser = UI.createBrowser(shell, this);
		Point parentSize = parent.getSize();
		shell.setSize((int) (parentSize.x * 0.85), (int) (parentSize.y * 0.85));
		UI.center(parent, shell);
	}

	private void openShell() {
		shell.open();
	}

	@Override
	public String getUrl() {
		return HtmlView.KML_EDITOR.getUrl();
	}

	@Override
	public void onLoaded() {
		registerSaveFunction();
		if (kml == null)
			return;
		String call = "setKML('" + kml + "')";
		try {
			browser.evaluate(call);
		} catch (Exception e) {
			log.error("failed to set KML data", e);
		}
	}

	private void registerSaveFunction() {
		new BrowserFunction(browser, "doSave") {
			@Override
			public Object function(Object[] args) {
				if (args == null || args.length == 0 || args[0] == null) {
					kml = null;
					return null;
				}
				if (handler != null)
					handler.contentSaved(args[0].toString());
				return null;
			}
		};
	}
}
