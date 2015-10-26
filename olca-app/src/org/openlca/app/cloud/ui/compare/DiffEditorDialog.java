package org.openlca.app.cloud.ui.compare;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.util.UI;

public class DiffEditorDialog extends FormDialog {

	public final static int KEEP_LOCAL_MODEL = 2;
	public final static int FETCH_REMOTE_MODEL = 3;
	private DiffEditor editor;
	private JsonNode root;
	private boolean editMode;

	public static DiffEditorDialog forEditing(JsonNode root) {
		DiffEditorDialog dialog = new DiffEditorDialog(root);
		dialog.editMode = true;
		return dialog;
	}

	public static DiffEditorDialog forViewing(JsonNode root) {
		DiffEditorDialog dialog = new DiffEditorDialog(root);
		dialog.editMode = false;
		return dialog;
	}

	private DiffEditorDialog(JsonNode root) {
		super(UI.shell());
		this.root = root;
		setBlockOnOpen(true);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1000, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, "#Diff");
		FormToolkit toolkit = mform.getToolkit();
		Composite body = form.getBody();
		UI.gridLayout(body, 1, 0, 0);
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		if (editMode)
			editor = DiffEditor.forEditing(body, toolkit, root);
		else
			editor = DiffEditor.forViewing(body, toolkit, root);
		UI.gridData(editor, true, true);
		form.reflow(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		boolean hasLocal = root.getLocalElement() != null;
		boolean hasRemote = root.getRemoteElement() != null;
		if (!editMode)
			createButton(parent, IDialogConstants.OK_ID, "#Close", true);
		else if (hasLocal && hasRemote)
			createButton(parent, IDialogConstants.OK_ID, "#Mark as merged",
					true);
		else if (hasLocal) {
			createButton(parent, KEEP_LOCAL_MODEL, "#Keep model deleted", true);
			createButton(parent, FETCH_REMOTE_MODEL, "#Fetch remote model",
					true);
		} else {
			createButton(parent, KEEP_LOCAL_MODEL, "#Keep local model", true);
			createButton(parent, FETCH_REMOTE_MODEL, "#Delete local model",
					true);
		}
	}

	public boolean localDiffersFromRemote() {
		return !editor.getRootNode().hasEqualValues();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (buttonId == KEEP_LOCAL_MODEL || buttonId == FETCH_REMOTE_MODEL) {
			setReturnCode(buttonId);
			close();
		}
	}

}
