package org.openlca.app.cloud.ui;

import org.openlca.app.M;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.diff.CommitDiffViewer;
import org.openlca.app.cloud.ui.diff.DiffNode;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;

public class CommitDialog extends FormDialog {

	private DiffNode node;
	private String message;
	private CommitDiffViewer viewer;
	private RepositoryClient client;
	private Set<String> initialSelection = new HashSet<>();

	public CommitDialog(DiffNode node, RepositoryClient client) {
		super(UI.shell());
		this.node = node;
		this.client = client;
		setBlockOnOpen(true);
	}

	public void setInitialSelection(Set<String> initialSelection) {
		this.initialSelection = initialSelection;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
				M.CommitChangesToRepository);
		FormToolkit toolkit = mform.getToolkit();
		Composite body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		createCommitMessage(body, toolkit);
		createModelViewer(body, toolkit);
		form.reflow(true);
		viewer.setInput(Collections.singleton(node));
		viewer.setInitialSelection(initialSelection);
	}

	private void createCommitMessage(Composite parent, FormToolkit toolkit) {
		Section section = UI.section(parent, toolkit, M.CommitMessage);
		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout());
		section.setClient(client);
		Text commitText = toolkit.createText(client, null, SWT.BORDER
				| SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		GridData gd = UI.gridData(commitText, true, false);
		gd.heightHint = 150;
		commitText.addModifyListener((event) -> {
			message = commitText.getText();
			updateButton();
		});
	}

	private void createModelViewer(Composite parent, FormToolkit toolkit) {
		Section section = UI.section(parent, toolkit, M.Files);
		UI.gridData(section, true, true);
		Composite comp = toolkit.createComposite(section);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 1);
		section.setClient(comp);
		JsonLoader loader = CloudUtil.getJsonLoader(client);
		viewer = new CommitDiffViewer(comp, loader);
		viewer.getViewer().addCheckStateListener((e) -> updateButton());
	}

	private void updateButton() {
		boolean enabled = viewer.hasChecked();
		if (message == null || message.isEmpty())
			enabled = false;
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, IDialogConstants.OK_ID, M.Commit, true)
				.setEnabled(false);
	}

	public String getMessage() {
		return message;
	}

	public List<DiffResult> getSelected() {
		List<DiffResult> selected = new ArrayList<>();
		for (DiffNode node : viewer.getChecked())
			selected.add(node.getContent());
		return selected;
	}

}
