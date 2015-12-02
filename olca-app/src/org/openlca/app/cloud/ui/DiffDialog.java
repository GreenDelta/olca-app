package org.openlca.app.cloud.ui;

import java.util.Collections;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.cloud.ui.diff.DiffNode;
import org.openlca.app.cloud.ui.diff.DiffTreeViewer;
import org.openlca.app.util.UI;

public class DiffDialog extends FormDialog {

	private DiffNode rootNode;
	private DiffTreeViewer viewer;
	private JsonLoader loader;

	public DiffDialog(DiffNode rootNode, JsonLoader loader) {
		super(UI.shell());
		this.rootNode = rootNode;
		this.loader = loader;
		setBlockOnOpen(true);
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
		viewer = new DiffTreeViewer(body, Direction.RIGHT_TO_LEFT, loader);
		form.reflow(true);
		viewer.setInput(Collections.singletonList(rootNode));
		viewer.setOnMerge(() -> getButton(OK)
				.setEnabled(!viewer.hasConflicts()));
	}

	@Override
	protected Button createButton(Composite parent, int id, String label,
			boolean defaultButton) {
		Button button = super.createButton(parent, id, label, defaultButton);
		if (id == OK)
			button.setEnabled(!viewer.hasConflicts());
		return button;
	}

}
