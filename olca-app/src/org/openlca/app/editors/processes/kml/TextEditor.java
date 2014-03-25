package org.openlca.app.editors.processes.kml;

import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextEditor {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Shell shell;
	private String kml;
	private EditorHandler handler;
	private StyledText styledText;

	public static void open(String kml, EditorHandler handler) {
		TextEditor editor = new TextEditor(kml, handler);
		editor.shell.open();
	}

	private TextEditor(String kml, EditorHandler handler) {
		this.kml = kml;
		this.handler = handler;
		Shell parent = UI.shell();
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(new FillLayout());
		shell.setText("KML Editor");
		styledText = new StyledText(shell, SWT.BORDER);
		Point parentSize = parent.getSize();
		shell.setSize((int) (parentSize.x * 0.6), (int) (parentSize.y * 0.6));
		UI.center(parent, shell);
		if (kml != null)
			initText();
	}

	private void initText() {
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new StringReader(kml));
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			StringWriter writer = new StringWriter();
			outputter.output(doc, writer);
			styledText.setText(writer.toString());
		} catch (Exception e) {
			log.error("failed to init. text", e);
		}
	}

}
