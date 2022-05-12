package org.openlca.app.collaboration.dialogs;

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
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.FetchViewer;
import org.openlca.app.util.UI;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.util.TypeRefIdMap;

public class FetchDialog extends FormDialog {

	private DiffNode rootNode;
	private FetchViewer viewer;
	private static final int OVERWRITE_LOCAL = 2;
	private static final int KEEP_LOCAL = 3;

	public FetchDialog(DiffNode rootNode) {
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
		ScrolledForm form = UI.formHeader(mform, M.Diff);
		FormToolkit toolkit = mform.getToolkit();
		Composite body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		viewer = new FetchViewer(body);
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
		createButton(parent, OVERWRITE_LOCAL, M.DiscardLocalChanges, false);
		createButton(parent, KEEP_LOCAL, M.OverwriteRemoteChanges, false);
		super.createButtonsForButtonBar(parent);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OVERWRITE_LOCAL) {
			solveAll(ConflictResolution.overwriteLocal());
		} else if (buttonId == KEEP_LOCAL) {
			solveAll(ConflictResolution.keepLocal());
		} else {
			super.buttonPressed(buttonId);
		}
	}

	public TypeRefIdMap<ConflictResolution> getResolvedConflicts() {
		return viewer.getResolvedConflicts();
	}

	private void solveAll(ConflictResolution resolution) {
		viewer.getResolvedConflicts().clear();
		for (DiffNode node : viewer.getConflicts()) {
			var r = node.contentAsTriDiff();
			viewer.getResolvedConflicts().put(r.type, r.refId, resolution);
		}
		viewer.refresh();
		getButton(OK).setEnabled(true);
		getButton(OVERWRITE_LOCAL).setEnabled(false);
		getButton(KEEP_LOCAL).setEnabled(false);
	}

}
