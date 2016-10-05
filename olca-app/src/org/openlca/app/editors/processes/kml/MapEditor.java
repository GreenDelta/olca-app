package org.openlca.app.editors.processes.kml;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.M;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.web.WebEngine;

public class MapEditor implements WebPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Shell shell;
	private WebEngine webkit;
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
		UI.createWebView(shell, this);
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
	public void onLoaded(WebEngine webkit) {
		this.webkit = webkit;
		UI.bindVar(webkit, "java", new JsHandler());
		UI.bindVar(webkit, "prettifier", new KmlPrettifyFunction(b -> {
		}));
		if (kml == null)
			kml = "";
		try {
			webkit.executeScript("setKML('" + kml + "')");
			webkit.executeScript("setOpenButtonVisible(" + handler.hasModel() + ")");
		} catch (Exception e) {
			log.error("failed to set KML data", e);
		}
	}

	public class JsHandler {

		public void doSave(String kml) {
			if (handler == null)
				return;
			boolean isValid = (Boolean) webkit.executeScript("isValidKml();");
			if (!isValid) {
				Info.showBox("The kml you provided is not valid, please check your input");
				return;
			}
			if (handler.contentSaved(kml)) {
				handler.openModel();
				close();
			}
		}

		public void doOpenEditor() {
			if (handler == null)
				return;
			handler.openModel();
			close();
		}
	}
}
