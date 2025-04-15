package org.openlca.app.collaboration.dialogs;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.viewers.diff.CommitViewer;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.trees.CheckboxTreeViewers;
import org.openlca.git.model.TriDiff;

public class CommitReferencesDialog extends FormDialog {

	private CommitViewer viewer;
	private Set<TriDiff> references;
	private boolean isStashCommit;

	public CommitReferencesDialog(Set<TriDiff> references, boolean isStashCommit) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.references = references;
		this.isStashCommit = isStashCommit;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.header(mform, M.CommitReferenceNotice);
		var toolkit = mform.getToolkit();
		var body = UI.body(form, toolkit);
		createModelViewer(body);
		form.reflow(true);
	}

	private void createModelViewer(Composite parent) {
		viewer = new CommitViewer(parent, Repository.get());
		var node = new DiffNodeBuilder(Database.get()).build(references);
		var selection = new HashSet<String>();
		references.stream()
				.filter(diff -> diff.right != null)
				.map(diff -> diff.right.path)
				.forEach(selection::add);
		viewer.setSelection(selection, node);
		CheckboxTreeViewers.setInput(
				parent, viewer.getViewer(), node, () -> CheckboxTreeViewers.expandGrayed(viewer.getViewer()));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		var selectAll = createButton(parent, IDialogConstants.CLIENT_ID + 1, M.SelectAll, false);
		Controls.onSelect(selectAll, (e) -> {
			viewer.selectAll();
		});
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, IDialogConstants.OK_ID, isStashCommit ? M.Stash : M.Commit, true);
	}

	public Set<DiffNode> getSelected() {
		return viewer.getChecked();
	}

}
