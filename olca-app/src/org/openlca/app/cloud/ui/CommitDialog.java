package org.openlca.app.cloud.ui;

import java.util.Collections;
import java.util.List;

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
import org.openlca.app.cloud.navigation.RepositoryNavigator;
import org.openlca.app.cloud.ui.DiffNodeBuilder.Node;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;

public class CommitDialog extends FormDialog {

	private final List<DiffResult> changes;
	private String message;
	private DiffTreeViewer viewer;

	public CommitDialog(List<DiffResult> changes) {
		super(UI.shell());
		this.changes = changes;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
				"#Commit changes to repository");
		FormToolkit toolkit = mform.getToolkit();
		Composite body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		createCommitMessage(body, toolkit);
		createModelViewer(body, toolkit);
		form.reflow(true);
		viewer.setInput(Collections.singleton(createModel()));
	}

	private Node createModel() {
		// TODO
		return new DiffNodeBuilder(Database.get(),
				RepositoryNavigator.getDiffIndex()).build(changes);
	}

	private void createCommitMessage(Composite parent, FormToolkit toolkit) {
		Section section = UI.section(parent, toolkit, "#Commit message");
		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout());
		section.setClient(client);
		Text commitText = toolkit.createText(client, null, SWT.BORDER
				| SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		GridData gd = UI.gridData(commitText, true, false);
		gd.heightHint = 150;
		commitText.addModifyListener((event) -> message = commitText.getText());
	}

	private void createModelViewer(Composite parent, FormToolkit toolkit) {
		Section section = UI.section(parent, toolkit, "#Files");
		UI.gridData(section, true, true);
		Composite client = toolkit.createComposite(section);
		UI.gridData(client, true, true);
		UI.gridLayout(client, 1);
		section.setClient(client);
		viewer = new DiffTreeViewer(client);
	}

	@Override
	protected void okPressed() {
		super.okPressed();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, IDialogConstants.OK_ID, "#Commit", true);
	}

	public String getMessage() {
		return message;
	}

	public List<DiffResult> getSelection() {
		return changes;
	}

}
