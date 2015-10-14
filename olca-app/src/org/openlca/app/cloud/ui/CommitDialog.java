package org.openlca.app.cloud.ui;

import java.util.Collections;

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
import org.openlca.app.cloud.CloudUtil.JsonLoader;
import org.openlca.app.cloud.ui.DiffNodeBuilder.DiffNode;
import org.openlca.app.util.UI;

import com.greendelta.cloud.api.RepositoryClient;

public class CommitDialog extends FormDialog {

	private DiffNode node;
	private String message;
	private DiffTreeViewer viewer;
	private RepositoryClient client;

	public CommitDialog(DiffNode node, RepositoryClient client) {
		super(UI.shell());
		this.node = node;
		this.client = client;
		setBlockOnOpen(true);
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
		viewer.setInput(Collections.singleton(node));
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
		Composite composite = toolkit.createComposite(section);
		UI.gridData(composite, true, true);
		UI.gridLayout(composite, 1);
		section.setClient(composite);
		JsonLoader loader = CloudUtil.getJsonLoader(client);
		viewer = new DiffTreeViewer(composite, loader::getLocalJson,
				loader::getRemoteJson);
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

}
