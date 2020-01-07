package org.openlca.app.cloud.ui.compare.json;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.compare.json.viewer.label.IJsonNodeLabelProvider;
import org.openlca.app.cloud.ui.diff.ActionType;
import org.openlca.app.util.UI;

public class DiffEditorDialog extends FormDialog {

	public final static int KEEP_LOCAL_MODEL = 2;
	public final static int FETCH_REMOTE_MODEL = 3;
	private DiffEditor editor;
	private JsonNode root;
	private boolean editMode;
	private ActionType action;
	private IJsonNodeLabelProvider labelProvider;
	private IDependencyResolver dependencyResolver;
	private String title;
	private Image logo;

	public static DiffEditorDialog forEditing(JsonNode root,
			IJsonNodeLabelProvider labelProvider,
			IDependencyResolver dependencyResolver, ActionType action) {
		DiffEditorDialog dialog = new DiffEditorDialog(root);
		dialog.labelProvider = labelProvider;
		dialog.dependencyResolver = dependencyResolver;
		dialog.editMode = true;
		dialog.action = action;
		return dialog;
	}

	public static DiffEditorDialog forViewing(JsonNode root,
			IJsonNodeLabelProvider labelProvider,
			IDependencyResolver dependencyResolver, ActionType action) {
		DiffEditorDialog dialog = new DiffEditorDialog(root);
		dialog.labelProvider = labelProvider;
		dialog.dependencyResolver = dependencyResolver;
		dialog.editMode = false;
		dialog.action = action;
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

	public void setTitle(String title) {
		this.title = title;
	}

	public void setLogo(Image logo) {
		this.logo = logo;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		String title = M.Diff;
		if (this.title != null)
			title += ": " + this.title;
		ScrolledForm form = UI.formHeader(mform, title);
		if (logo != null)
			form.setImage(logo);
		FormToolkit toolkit = mform.getToolkit();
		Composite body = form.getBody();
		UI.gridLayout(body, 1, 0, 0);
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		if (editMode)
			editor = DiffEditor.forEditing(body, toolkit);
		else
			editor = DiffEditor.forViewing(body, toolkit);
		editor.initialize(root, labelProvider, dependencyResolver, action);
		UI.gridData(editor, true, true);
		form.reflow(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		boolean hasLeft = root.localElement != null;
		boolean hasRight = root.remoteElement != null;
		if (!editMode)
			createButton(parent, IDialogConstants.OK_ID, M.Close, true);
		else if (hasLeft && hasRight)
			createButton(parent, IDialogConstants.OK_ID, M.MarkAsMerged, true);
		else if (hasRight) {
			createButton(parent, KEEP_LOCAL_MODEL, M.KeepModelDeleted, true);
			createButton(parent, FETCH_REMOTE_MODEL, M.FetchRemoteModel, false);
		} else {
			createButton(parent, KEEP_LOCAL_MODEL, M.KeepLocalModel, false);
			createButton(parent, FETCH_REMOTE_MODEL, M.DeleteLocalModel, true);
		}
	}

	public boolean leftDiffersFromRight() {
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
