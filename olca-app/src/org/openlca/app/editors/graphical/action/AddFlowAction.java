package org.openlca.app.editors.graphical.action;

import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.jface.action.Action;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.ModelType;

public class AddFlowAction extends Action implements UpdateAction {

	public static final String ID = "AddFlowAction";
	private final GraphEditor editor;
	private ProcessNode processNode;

	public AddFlowAction(GraphEditor editor) {
		this.editor = editor;
		setId(ID);
		setText("Add a flow");
		setImageDescriptor(Images.descriptor(ModelType.FLOW));
	}

	@Override
	public void update() {
		if (editor == null) {
			setEnabled(false);
			return;
		}
		processNode = GraphActions.firstSelectedOf(
				editor, ProcessNode.class);
		if (processNode == null) {
			setEnabled(false);
			return;
		}
		var d = processNode.process;
		setEnabled(d != null && d.type == ModelType.PROCESS);
	}

	@Override
	public void run() {
		if (processNode == null)
			return;
		var d = processNode.process;
		if (d == null || d.type != ModelType.PROCESS)
			return;
		// 1. open the dialog that returns a flow
		// 2. ask for an amount
		// 3. add the exchange to the process
		// 4. update the process
	}

}
