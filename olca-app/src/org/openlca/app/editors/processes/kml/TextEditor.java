package org.openlca.app.editors.processes.kml;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;

public class TextEditor {

	private final Shell shell;
	private EditorHandler handler;
	private StyledText styledText;

	public static void open(String kml, EditorHandler handler) {
		TextEditor editor = new TextEditor(kml, handler);
		editor.shell.open();
	}

	private TextEditor(String kml, EditorHandler handler) {
		this.handler = handler;
		Shell parent = UI.shell();
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setLayout(new FillLayout());
		shell.setText(Messages.KmlEditor);
		styledText = new StyledText(shell, SWT.BORDER);
		Point parentSize = parent.getSize();
		shell.setSize((int) (parentSize.x * 0.6), (int) (parentSize.y * 0.6));
		UI.center(parent, shell);
		if (kml != null)
			styledText.setText(KmlUtil.prettyFormat(kml));
	}
}
