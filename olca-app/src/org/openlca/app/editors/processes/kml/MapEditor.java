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
	private String kml;
	private EditorHandler handler;
	private boolean editOnly;

	public static void open(String name, String kml, EditorHandler handler) {
		MapEditor editor = new MapEditor(name, kml, false, handler);
		editor.openShell();
	}

	/**
	 * Opens the map editor. Only the update button is available. 'Save as'
	 * button and name input are not shown
	 */
	public static void openForEditingOnly(String kml, EditorHandler handler) {
		MapEditor editor = new MapEditor(null, kml, true, handler);
		editor.openShell();
	}

	private MapEditor(String name, String kml, boolean editOnly,
			EditorHandler handler) {
		this.kml = kml;
		this.editOnly = editOnly;
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

	public void close() {
		shell.close();
	}

	@Override
	public IHtmlResource getResource() {
		return HtmlView.KML_EDITOR.getResource();
	}

	@Override
	public void onLoaded() {
		registerSaveFunction();
		registerPrettifyFunction();
		registerTransformFunction();
		if (kml == null)
			kml = "";
		try {
			browser.evaluate("setKML('" + kml + "')");
			if (editOnly)
				browser.evaluate("setEditOnly()");
		} catch (Exception e) {
			log.error("failed to set KML data", e);
		}
	}

	private void registerSaveFunction() {
		new BrowserFunction(browser, "doSave") {
			@Override
			public Object function(Object[] args) {
				if (handler == null)
					return null;
				String kml = getArg(args, 0);
				Boolean overwrite = getArg(args, 1);
				if (overwrite == null)
					return null;
				handler.contentSaved(MapEditor.this, kml, overwrite);
				return null;
			}
		};
	}

	private void registerPrettifyFunction() {
		new BrowserFunction(browser, "prettifyKML") {
			@Override
			public Object function(Object[] arguments) {
				String kml = getArg(arguments, 0);
				if (kml == null || kml.isEmpty())
					return null;
				return KmlUtil.prettyFormat(kml);
			}
		};
	}

	private void registerTransformFunction() {
		new BrowserFunction(browser, "transformKML") {
			@Override
			public Object function(Object[] arguments) {
				String kml = getArg(arguments, 0);
				if (kml == null || kml.isEmpty())
					return null;
				return KmlUtil.transformMultiPlacemarks(kml);
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
