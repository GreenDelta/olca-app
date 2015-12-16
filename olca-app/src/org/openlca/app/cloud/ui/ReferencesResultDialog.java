package org.openlca.app.cloud.ui;

import java.util.Collections;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.diff.DiffNode;
import org.openlca.app.cloud.ui.diff.ReferencesDiffViewer;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;

public class ReferencesResultDialog extends FormDialog {

	private ReferencesDiffViewer viewer;
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
		ScrolledForm form = UI.formHeader(mform,
				"#Referenced changes that need to be committed as well");
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
		viewer = new ReferencesDiffViewer(parent, loader);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		createButton(parent, IDialogConstants.OK_ID, "#Accept && commit", true);
	}

}
