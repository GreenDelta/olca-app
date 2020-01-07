package org.openlca.app.cloud.ui;

import java.util.Collections;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.diff.DiffNode;
import org.openlca.app.cloud.ui.diff.FetchDiffViewer;
import org.openlca.app.util.UI;

public class DiffDialog extends FormDialog {

	private DiffNode rootNode;
	private FetchDiffViewer viewer;
	private JsonLoader loader;
	private static final int DISCARD_LOCAL_CHANGES = 2;
	private static final int OVERWRITE_REMOTE_CHANGES = 3;

	public DiffDialog(DiffNode rootNode, JsonLoader loader) {
		super(UI.shell());
		this.rootNode = rootNode;
		this.loader = loader;
		setBlockOnOpen(true);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, M.Diff);
		FormToolkit toolkit = mform.getToolkit();
		Composite body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		viewer = new FetchDiffViewer(body, loader);
		form.reflow(true);
		viewer.setInput(Collections.singletonList(rootNode));
		viewer.setOnMerge(() -> getButton(OK).setEnabled(!viewer.hasConflicts()));
	}

	@Override
	protected Button createButton(Composite parent, int id, String label,
			boolean defaultButton) {
		Button button = super.createButton(parent, id, label, defaultButton);
		if (id == OK)
			button.setEnabled(!viewer.hasConflicts());
		return button;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, DISCARD_LOCAL_CHANGES, M.DiscardLocalChanges, false);
		createButton(parent, OVERWRITE_REMOTE_CHANGES, M.OverwriteRemoteChanges, false);
		super.createButtonsForButtonBar(parent);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == DISCARD_LOCAL_CHANGES) {
			discardLocalChanges();
		} else if (buttonId == OVERWRITE_REMOTE_CHANGES) {
			overwriteRemoteChanges();
		} else {
			super.buttonPressed(buttonId);
		}
	}
	
	private void discardLocalChanges() {
		for (DiffNode node : viewer.getConflicts()) {
			node.getContent().mergedData = null;
			node.getContent().overwriteRemoteChanges = false;
			node.getContent().overwriteLocalChanges = true;
		}
		viewer.refresh();
		getButton(OK).setEnabled(true);
		getButton(DISCARD_LOCAL_CHANGES).setEnabled(false);
		getButton(OVERWRITE_REMOTE_CHANGES).setEnabled(false);
	}
	
	private void overwriteRemoteChanges() {
		for (DiffNode node : viewer.getConflicts()) {
			node.getContent().mergedData = null;
			node.getContent().overwriteLocalChanges = false;
			node.getContent().overwriteRemoteChanges = true;
		}
		viewer.refresh();
		getButton(OK).setEnabled(true);
		getButton(DISCARD_LOCAL_CHANGES).setEnabled(false);
		getButton(OVERWRITE_REMOTE_CHANGES).setEnabled(false);
	}

}
