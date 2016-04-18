package org.openlca.app.cloud.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.diff.CommitDiffViewer;
import org.openlca.app.cloud.ui.diff.DiffNode;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;

public class ReferencesResultDialog extends FormDialog {

	private CommitDiffViewer viewer;
	private DiffNode node;
	private RepositoryClient client;

	public ReferencesResultDialog(DiffNode node, RepositoryClient client) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.node = node;
		this.client = client;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, M.CommitReferenceNotice);
		FormToolkit toolkit = mform.getToolkit();
		Composite body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		createModelViewer(body, toolkit);
		form.reflow(true);
		viewer.setInput(Collections.singleton(node));
	}

	private void createModelViewer(Composite parent, FormToolkit toolkit) {
		JsonLoader loader = CloudUtil.getJsonLoader(client);
		viewer = new CommitDiffViewer(parent, loader);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, IDialogConstants.OK_ID, M.Commit, true);
	}

	public List<DiffResult> getSelected() {
		List<DiffResult> selected = new ArrayList<>();
		for (DiffNode node : viewer.getChecked())
			selected.add(node.getContent());
		return selected;
	}

}
