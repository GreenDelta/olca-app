package org.openlca.app.cloud.ui;

import java.util.Collections;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.cloud.ui.DiffNodeBuilder.Node;
import org.openlca.app.util.UI;

public class DiffDialog extends FormDialog {

	private Node rootNode;
	private DiffTreeViewer viewer;

	public DiffDialog(Node rootNode) {
		super(UI.shell());
		this.rootNode = rootNode;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, "#Diff");
		FormToolkit toolkit = mform.getToolkit();
		Composite body = form.getBody();
		body.setLayout(new GridLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		viewer = new DiffTreeViewer(body);
		form.reflow(true);
		viewer.setInput(Collections.singletonList(rootNode));
	}

}
