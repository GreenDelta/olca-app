package org.openlca.app.cloud.ui;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Commit;

public class CommitEntryDialog extends FormDialog {

	private final List<Commit> commits;
	private CommitEntryViewer viewer;
	private RepositoryClient client;

	public CommitEntryDialog(List<Commit> commits, RepositoryClient client) {
		super(UI.shell());
		this.commits = commits;
		this.client = client;
		setBlockOnOpen(true);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, "#Fetched changes");
		FormToolkit toolkit = mform.getToolkit();
		Composite body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		createCommitViewer(body, toolkit);
		form.reflow(true);
		viewer.setInput(commits);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

	private void createCommitViewer(Composite parent, FormToolkit toolkit) {
		viewer = new CommitEntryViewer(parent, client);
	}

}
