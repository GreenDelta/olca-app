package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;

public class AddProcessAction extends Action {

	public static final String ID = "AddProcessAction";
	private final GraphEditor editor;

	public AddProcessAction(GraphEditor editor) {
		this.editor = editor;
		setId(ID);
		setText("Add a process");
		setImageDescriptor(Images.descriptor(ModelType.PROCESS));
	}

	@Override
	public void run() {
		var d = new Dialog();
		d.open();
	}

	private class Dialog extends FormDialog {

		TreeViewer tree;
		Text text;

		Dialog() {
			super(UI.shell());
			setBlockOnOpen(true);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			UI.gridLayout(body, 1);
			var nameLabel = UI.formLabel(body, tk, "Create with name");
			nameLabel.setFont(UI.boldFont());
			text = UI.formText(body, SWT.NONE);
			var selectLabel = UI.formLabel(body, tk, "Or select existing");
			selectLabel.setFont(UI.boldFont());
			tree = NavigationTree.forSingleSelection(body, ModelType.PROCESS);
			UI.gridData(tree.getControl(), true, true);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
		}

		@Override
		protected Point getInitialSize() {
			var shell = getShell().getDisplay().getBounds();
			int width = shell.x > 0 && shell.x < 600
					? shell.x
					: 600;
			int height = shell.y > 0 && shell.y < 600
					? shell.y
					: 600;
			return new Point(width, height);
		}
	}
}
