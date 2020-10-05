package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.rcp.images.Images;
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
		System.out.println("open process wizard...");
	}
}
