package org.openlca.app.collaboration.dialogs;

import java.util.Collections;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.MergeViewer;
import org.openlca.app.util.UI;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.util.TypeRefIdMap;

public class MergeDialog extends FormDialog {

	private DiffNode rootNode;
	private MergeViewer viewer;
	private static final int OVERWRITE = 2;
	private static final int KEEP = 3;

	public MergeDialog(DiffNode rootNode) {
		super(UI.shell());
		this.rootNode = rootNode;
		setBlockOnOpen(true);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform, M.Merge);
		var toolkit = mform.getToolkit();
		var body = UI.formBody(form, toolkit);
		viewer = new MergeViewer(body);
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
		createButton(parent, OVERWRITE, M.OverwriteLocalChanges, false);
		createButton(parent, KEEP, M.OverwriteRemoteChanges, false);
		super.createButtonsForButtonBar(parent);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OVERWRITE) {
			solveAll(ConflictResolution.overwrite());
		} else if (buttonId == KEEP) {
			solveAll(ConflictResolution.keep());
		} else {
			super.buttonPressed(buttonId);
		}
	}

	public TypeRefIdMap<ConflictResolution> getResolvedConflicts() {
		return viewer.getResolvedConflicts();
	}

	private void solveAll(ConflictResolution resolution) {
		viewer.getResolvedConflicts().clear();
		viewer.getConflicts().stream()
				.map(DiffNode::contentAsTriDiff)
				.forEach(d -> viewer.getResolvedConflicts().put(d, resolution));
		viewer.refresh();
		getButton(OK).setEnabled(true);
		getButton(OVERWRITE).setEnabled(false);
		getButton(KEEP).setEnabled(false);
	}

}
