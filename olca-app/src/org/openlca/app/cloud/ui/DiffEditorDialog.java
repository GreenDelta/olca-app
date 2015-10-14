package org.openlca.app.cloud.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.util.UI;

import com.google.gson.JsonObject;

public class DiffEditorDialog extends FormDialog {

	private JsonObject local;
	private JsonObject remote;
	private DiffEditor editor;

	public DiffEditorDialog(JsonObject local, JsonObject remote) {
		super(UI.shell());
		this.local = local;
		this.remote = remote;
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
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		editor = new DiffEditor(body, toolkit);
		UI.gridData(editor, true, true);
		form.reflow(true);
		editor.setInput(local, remote);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

}
