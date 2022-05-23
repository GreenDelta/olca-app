package org.openlca.app.collaboration.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.collaboration.viewers.diff.CommitViewer;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.trees.CheckboxTreeViewers;
import org.openlca.git.util.TypeRefIdSet;

public class CommitDialog extends FormDialog {

	public static final int COMMIT_AND_PUSH = 2;
	private final boolean canPush;
	private final DiffNode node;
	private String message;
	private CommitViewer viewer;
	private TypeRefIdSet initialSelection;

	public CommitDialog(DiffNode node, boolean canPush) {
		super(UI.shell());
		this.node = node;
		this.canPush = canPush;
		setBlockOnOpen(true);
	}

	public void setInitialSelection(TypeRefIdSet initialSelection) {
		this.initialSelection = initialSelection;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform, M.CommitChangesToRepository);
		var toolkit = mform.getToolkit();
		var body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		createCommitMessage(body, toolkit);
		createModelViewer(body, toolkit);
		form.reflow(true);
	}

	private void createCommitMessage(Composite parent, FormToolkit toolkit) {
		var section = UI.section(parent, toolkit, M.CommitMessage + "*");
		var client = toolkit.createComposite(section);
		client.setLayout(new GridLayout());
		section.setClient(client);
		Text commitText = toolkit.createText(client, null, SWT.BORDER
				| SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		var gd = UI.gridData(commitText, true, false);
		gd.heightHint = 150;
		commitText.addModifyListener((event) -> {
			message = commitText.getText();
			updateButton();
		});
	}

	private void createModelViewer(Composite parent, FormToolkit toolkit) {
		var section = UI.section(parent, toolkit, M.Files + "*");
		UI.gridData(section, true, true);
		var comp = toolkit.createComposite(section);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 1);
		section.setClient(comp);
		viewer = new CommitViewer(comp, this::updateButton);
		viewer.setSelection(initialSelection, node);
		CheckboxTreeViewers.registerInputHandler(comp, viewer.getViewer(), node, () -> {
			CheckboxTreeViewers.expandGrayed(viewer.getViewer());
			this.updateButton();
		});
	}

	private void updateButton() {
		var enabled = viewer.hasChecked();
		if (message == null || message.isEmpty()) {
			enabled = false;
		}
		if (canPush) {
			getButton(COMMIT_AND_PUSH).setEnabled(enabled);
		}
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		if (canPush) {
			var commitAndPush = createButton(parent, COMMIT_AND_PUSH, M.CommitAndPush, false);
			commitAndPush.setEnabled(false);
			commitAndPush.setImage(Icon.PUSH.get());
			setButtonLayoutData(commitAndPush);
		}
		var commit = createButton(parent, IDialogConstants.OK_ID, M.Commit, true);
		commit.setEnabled(false);
		commit.setImage(Icon.COMMIT.get());
		setButtonLayoutData(commit);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == COMMIT_AND_PUSH) {
			setReturnCode(COMMIT_AND_PUSH);
			close();
		} else {
			super.buttonPressed(buttonId);
		}
	}

	public String getMessage() {
		return message;
	}

	public List<TriDiff> getSelected() {
		return viewer.getChecked().stream()
				.map(n -> n.contentAsTriDiff())
				.toList();
	}

}
