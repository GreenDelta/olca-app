package org.openlca.app.editors.processes.kml;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.html.HtmlPage;
import org.openlca.app.html.HtmlView;
import org.openlca.app.html.IHtmlResource;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapEditor implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Shell shell;
	private final Browser browser;
	private String name;
	private String kml;
	private EditorHandler handler;

	public static void open(String name, String kml, EditorHandler handler) {
		MapEditor editor = new MapEditor(name, kml, handler);
		editor.openShell();
	}

	private MapEditor(String name, String kml, EditorHandler handler) {
		this.name = name;
		this.kml = kml;
		this.handler = handler;
		Shell parent = UI.shell();
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(new FillLayout());
		shell.setText("KML Editor");
		browser = UI.createBrowser(shell, this);
		Point parentSize = parent.getSize();
		shell.setSize((int) (parentSize.x * 0.85), (int) (parentSize.y * 0.85));
		UI.center(parent, shell);
	}

	private void openShell() {
		shell.open();
	}

	@Override
	public IHtmlResource getResource() {
		return HtmlView.KML_EDITOR.getResource();
	}

	@Override
	public void onLoaded() {
		registerSaveFunction();
		if (kml == null)
			return;
		String setKml = "setKML('" + kml + "')";
		String setName = "setName('" + name + "')";
		try {
			browser.evaluate(setKml);
			browser.evaluate(setName);
		} catch (Exception e) {
			log.error("failed to set KML data", e);
		}
	}

	private void registerSaveFunction() {
		new BrowserFunction(browser, "doSave") {
			@Override
			public Object function(Object[] args) {
				String name = getArg(args, 0);
				String kml = getArg(args, 1);
				Boolean overwrite = getArg(args, 2);
				if (name == null || kml == null || overwrite == null) {
					kml = null;
					return null;
				}
				if (handler != null)
					handler.contentSaved(name, kml, overwrite);
				return null;
			}
		};
	}

	@SuppressWarnings("unchecked")
	private <T> T getArg(Object[] args, int index) {
		if (args.length <= index)
			return null;
		return (T) args[index];
	}
}
