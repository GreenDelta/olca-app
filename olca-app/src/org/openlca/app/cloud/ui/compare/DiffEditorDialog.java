package org.openlca.app.cloud.ui.compare;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.util.UI;

import com.google.gson.JsonObject;

public class DiffEditorDialog extends FormDialog {

	public final static int KEEP_LOCAL_MODEL = IDialogConstants.CANCEL_ID;
	public final static int FETCH_REMOTE_MODEL = IDialogConstants.OK_ID;
	private JsonObject local;
	private JsonObject remote;
	private JsonObject merged;
	private DiffEditor editor;
	private boolean editMode;

	public static DiffEditorDialog forEditing(JsonObject local,
			JsonObject remote, JsonObject merged) {
		DiffEditorDialog dialog = new DiffEditorDialog(local, remote, merged);
		dialog.editMode = true;
		return dialog;
	}

	public static DiffEditorDialog forViewing(JsonObject local,
			JsonObject remote) {
		DiffEditorDialog dialog = new DiffEditorDialog(local, remote, null);
		dialog.editMode = false;
		return dialog;
	}

	private DiffEditorDialog(JsonObject local, JsonObject remote,
			JsonObject merged) {
		super(UI.shell());
		this.local = local;
		this.remote = remote;
		this.merged = merged;
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
			editor = DiffEditor
					.forEditing(body, toolkit, local, remote, merged);
		else
			editor = DiffEditor.forViewing(body, toolkit, local, remote);
		UI.gridData(editor, true, true);
		form.reflow(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (!editMode)
			createButton(parent, IDialogConstants.OK_ID, "#Close", true);
		else if (local != null && remote != null)
			createButton(parent, IDialogConstants.OK_ID, "#Mark as merged",
					true);
		else if (local == null) {
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

}
