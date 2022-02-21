package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.viewers.json.JsonDiffViewer;
import org.openlca.app.collaboration.viewers.json.content.IDependencyResolver;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.collaboration.viewers.json.label.IJsonNodeLabelProvider;
import org.openlca.app.util.UI;

public class JsonDiffDialog extends FormDialog {

	public final static int KEEP_LOCAL_MODEL = 2;
	public final static int FETCH_REMOTE_MODEL = 3;
	private final JsonNode root;
	private final boolean editMode;
	private final Direction direction;
	private JsonDiffViewer viewer;
	private IJsonNodeLabelProvider labelProvider;
	private IDependencyResolver dependencyResolver;
	private String title;
	private Image logo;

	public static JsonDiffDialog forEditing(JsonNode root, Direction direction) {
		return new JsonDiffDialog(root, direction, true);
	}

	public static JsonDiffDialog forViewing(JsonNode root, Direction direction) {
		return new JsonDiffDialog(root, direction, false);
	}

	private JsonDiffDialog(JsonNode root, Direction direction, boolean editMode) {
		super(UI.shell());
		this.root = root;
		this.direction = direction;
		this.editMode = editMode;
		setBlockOnOpen(true);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var title = M.Diff;
		if (this.title != null) {
			title += ": " + this.title;
		}
		var form = UI.formHeader(mform, title);
		if (logo != null) {
			form.setImage(logo);
		}
		var toolkit = mform.getToolkit();
		var body = form.getBody();
		UI.gridLayout(body, 1, 0, 0);
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		viewer = editMode
				? JsonDiffViewer.forEditing(body, toolkit, root, direction)
				: JsonDiffViewer.forViewing(body, toolkit, root, direction);
		viewer.initialize(labelProvider, dependencyResolver);
		UI.gridData(viewer, true, true);
		form.reflow(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		var hasLeft = root.localElement != null;
		var hasRight = root.remoteElement != null;
		if (!editMode) {
			createButton(parent, IDialogConstants.OK_ID, M.Close, true);
		} else if (hasLeft && hasRight) {
			createButton(parent, IDialogConstants.OK_ID, M.MarkAsMerged, true);
		} else if (hasRight) {
			createButton(parent, KEEP_LOCAL_MODEL, M.KeepModelDeleted, true);
			createButton(parent, FETCH_REMOTE_MODEL, M.FetchRemoteModel, false);
		} else {
			createButton(parent, KEEP_LOCAL_MODEL, M.KeepLocalModel, false);
			createButton(parent, FETCH_REMOTE_MODEL, M.DeleteLocalModel, true);
		}
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

	public void setDependencyResolver(IDependencyResolver dependencyResolver) {
		this.dependencyResolver = dependencyResolver;
	}

	public void setLabelProvider(IJsonNodeLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}

	public boolean leftDiffersFromRight() {
		return !viewer.leftDiffersFromRight();
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
