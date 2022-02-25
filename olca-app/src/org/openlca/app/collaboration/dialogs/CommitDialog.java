package org.openlca.app.collaboration.dialogs;

import java.util.Collection;
import java.util.Collections;
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
import org.openlca.app.collaboration.util.TypeRefIdSet;
import org.openlca.app.collaboration.viewers.diff.CommitViewer;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.DiffResult;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;

public class CommitDialog extends FormDialog {

	public static final int COMMIT_AND_PUSH = 2;
	private DiffNode node;
	private String message;
	private CommitViewer viewer;
	private Collection<INavigationElement<?>> initialSelection;

	public CommitDialog(DiffNode node) {
		super(UI.shell());
		this.node = node;
		setBlockOnOpen(true);
	}

	public void setInitialSelection(Collection<INavigationElement<?>> initialSelection) {
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
		viewer.setInput(Collections.singleton(node));
		viewer.setSelection(getSelection());
	}

	private TypeRefIdSet getSelection() {
		if (initialSelection == null)
			return null;
		var map = new TypeRefIdSet();
		initialSelection.forEach(element -> put(map, element));
		return map;
	}

	private void put(TypeRefIdSet map, INavigationElement<?> element) {
		if (element instanceof ModelElement e) {
			var model = e.getContent();
			map.add(model.type, model.refId);
			return;
		}
		element.getChildren().forEach(child -> put(map, child));
	}

	private void createCommitMessage(Composite parent, FormToolkit toolkit) {
		var section = UI.section(parent, toolkit, M.CommitMessage);
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
		var section = UI.section(parent, toolkit, M.Files);
		UI.gridData(section, true, true);
		var comp = toolkit.createComposite(section);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 1);
		section.setClient(comp);
		viewer = new CommitViewer(comp, false);
		viewer.getViewer().addCheckStateListener((e) -> updateButton());
	}

	private void updateButton() {
		var enabled = viewer.hasChecked();
		if (message == null || message.isEmpty()) {
			enabled = false;
		}
		getButton(COMMIT_AND_PUSH).setEnabled(enabled);
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		var commitAndPush = createButton(parent, COMMIT_AND_PUSH, M.CommitAndPush, false);
		commitAndPush.setEnabled(false);
		commitAndPush.setImage(Icon.PUSH.get());
		setButtonLayoutData(commitAndPush);
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

	public List<DiffResult> getSelected() {
		return viewer.getChecked().stream()
				.map(n -> n.contentAsDiffResult())
				.filter(r -> r != null)
				.toList();
	}

}
