package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.util.UI;

public class EditStickyNoteDialog extends FormDialog {

	private final StickyNote note;
	private Text title;
	private Text content;

	/**
	 * Opens the dialog and returns true if the value of the
	 * exchange was changed.
	 */
	static boolean open(StickyNote note) {
		if (note == null)
			return false;
		var dialog = new EditStickyNoteDialog(note);
		return dialog.open() == OK;
	}

	private EditStickyNoteDialog(StickyNote note) {
		super(UI.shell());
		this.note = note;
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(M.Note);
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 600, 350);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 2);
		title = UI.formText(body, tk, M.Title);
		title.setText(note.title);

		content = UI.formText(body, tk, M.Content);
		content.setSize(content.getSize().x, 250);
		content.setText(note.content);
	}

	@Override
	protected void okPressed() {
		note.setTitle(title.getText());
		note.setContent(content.getText());
		super.okPressed();
	}

}
