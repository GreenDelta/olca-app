package org.openlca.app.collaboration.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.collaboration.viewers.diff.CommitViewer;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.trees.CheckboxTreeViewers;
import org.openlca.git.model.DiffType;
import org.openlca.git.util.TypeRefIdSet;

public class CommitReferencesDialog extends FormDialog {

	private CommitViewer viewer;
	private DiffNode node;
	private boolean isStashCommit;

	public CommitReferencesDialog(DiffNode node, boolean isStashCommit) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.node = node;
		this.isStashCommit = isStashCommit;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform, M.CommitReferenceNotice);
		var toolkit = mform.getToolkit();
		var body = UI.formBody(form, toolkit);
		createModelViewer(body, toolkit);
		form.reflow(true);
	}

	private TypeRefIdSet getNewElements(DiffNode node) {
		var newElements = new TypeRefIdSet();
		var diff = node.contentAsTriDiff();
		if (diff != null && diff.leftDiffType == DiffType.ADDED) {
			newElements.add(diff);
		}
		if (node.children == null)
			return newElements;
		node.children.forEach(child -> getNewElements(child)
				.forEach(newElements::add));
		return newElements;
	}

	private void createModelViewer(Composite parent, FormToolkit toolkit) {
		viewer = new CommitViewer(parent);
		var newElements = getNewElements(node);
		viewer.setLockedElements(newElements);
		viewer.setSelection(newElements, node);
		CheckboxTreeViewers.registerInputHandler(parent, viewer.getViewer(), node, () -> {
			CheckboxTreeViewers.expandGrayed(viewer.getViewer());
		});
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		var selectAll = createButton(parent, IDialogConstants.CLIENT_ID + 1, M.SelectAll, false);
		Controls.onSelect(selectAll, (e) -> {
			viewer.selectAll();
		});
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, IDialogConstants.OK_ID, isStashCommit ? "Stash" : M.Commit, true);
	}

	public List<TriDiff> getSelected() {
		return viewer.getChecked().stream()
				.map(DiffNode::contentAsTriDiff)
				.toList();
	}

}
