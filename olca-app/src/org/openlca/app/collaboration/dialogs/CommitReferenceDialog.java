package org.openlca.app.collaboration.dialogs;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.collaboration.util.ModelTypeRefIdMap;
import org.openlca.app.collaboration.viewers.diff.CommitViewer;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.DiffResult;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.Reference;

public class CommitReferenceDialog extends FormDialog {

	private CommitViewer viewer;
	private DiffNode node;

	public CommitReferenceDialog(DiffNode node) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.node = node;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform, M.CommitReferenceNotice);
		var toolkit = mform.getToolkit();
		var body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		createModelViewer(body, toolkit);
		form.reflow(true);
		viewer.setInput(Collections.singleton(node));
		var initialSelection = getNewElements(node);
		var map = new ModelTypeRefIdMap(initialSelection);
		viewer.setSelection(map);
	}

	private Set<Reference> getNewElements(DiffNode node) {
		var newElements = new HashSet<Reference>();
		var result = node.contentAsDiffResult();
		if (result != null && result.local.type == DiffType.ADDED) {
			newElements.add(result.ref());
		}
		if (node.children == null)
			return newElements;
		node.children.forEach(child -> newElements.addAll(getNewElements(child)));
		return newElements;
	}

	private void createModelViewer(Composite parent, FormToolkit toolkit) {
		viewer = new CommitViewer(parent, true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		var selectAll = createButton(parent, IDialogConstants.CLIENT_ID + 1, M.SelectAll, false);
		Controls.onSelect(selectAll, (e) -> {
			viewer.selectAll();
		});
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, IDialogConstants.OK_ID, M.Commit, true);
	}

	public List<DiffResult> getSelected() {
		return viewer.getChecked()
				.stream().map(n -> n.contentAsDiffResult())
				.toList();
	}

}
