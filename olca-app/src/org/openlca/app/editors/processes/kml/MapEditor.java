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

	private MapEditor(String name, String kml, boolean editOnly, EditorHandler handler) {
		this.kml = kml;
		this.editOnly = editOnly;
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

	public void close() {
		shell.close();
	}

	@Override
	public String getUrl() {
		return HtmlView.KML_EDITOR.getUrl();
	}

	@Override
	public void onLoaded() {
		new LocationSaveFunction(browser, handler, this::close);
		new KmlPrettifyFunction(browser);
		if (kml == null)
			kml = "";
		try {
			browser.evaluate("setKML('" + kml + "')");
			browser.evaluate("setEditOnly(" + editOnly + ")");
		} catch (Exception e) {
			log.error("failed to set KML data", e);
		}
	}

	public static class LocationSaveFunction extends BrowserFunction {

		private EditorHandler handler;
		private Runnable callback;

		public LocationSaveFunction(Browser browser, EditorHandler handler, Runnable callback) {
			super(browser, "doSave");
			this.handler = handler;
			this.callback = callback;
		}

		@Override
		public Object function(Object[] args) {
			if (handler == null)
				return null;
			String kml = getArg(args, 0);
			Boolean overwrite = getArg(args, 1);
			if (overwrite == null)
				return null;
			handler.contentSaved(kml, overwrite, callback);
			return null;
		}

		@SuppressWarnings("unchecked")
		private <T> T getArg(Object[] args, int index) {
			if (args.length <= index)
				return null;
			return (T) args[index];
		}

	}

	public static class KmlPrettifyFunction extends BrowserFunction {

		public KmlPrettifyFunction(Browser browser) {
			super(browser, "prettifyKML");
		}

		@Override
		public Object function(Object[] arguments) {
			String kml = getArg(arguments, 0);
			if (kml == null || kml.isEmpty())
				return null;
			return KmlUtil.prettyFormat(kml);
		}

		@SuppressWarnings("unchecked")
		private <T> T getArg(Object[] args, int index) {
			if (args.length <= index)
				return null;
			return (T) args[index];
		}

	}

}
