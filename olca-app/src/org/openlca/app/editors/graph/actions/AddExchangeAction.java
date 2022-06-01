package org.openlca.app.editors.graph.actions;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.editors.graph.edit.IOPaneEditPart;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.*;


public class AddExchangeAction extends SelectionAction {

	public static final String REQ_ADD_INPUT_EXCHANGE = "add_input_exchange";
	public static final String REQ_ADD_OUTPUT_EXCHANGE = "add_output_exchange";
	private final boolean forInput;
	Request request;

	public AddExchangeAction(GraphEditor part, boolean forInput) {
		super(part);
		this.forInput = forInput;
		request = new Request(forInput
			? REQ_ADD_INPUT_EXCHANGE
			: REQ_ADD_OUTPUT_EXCHANGE);
		setId(forInput
			?	ActionIds.ADD_INPUT_EXCHANGE
			: ActionIds.ADD_OUTPUT_EXCHANGE);
		// TODO (francois) NLS.bind does not seem to work.
		//		var name = NLS.bind(forInput ? M.Input : M.Output, M.Flow);
		//		setText(NLS.bind(M.Add, name));
		setText("Add " + (forInput ? "input" : "ouput") + " flow");
		setImageDescriptor(Images.descriptor(ModelType.FLOW));
	}

	@Override
	protected boolean calculateEnabled() {
		if (getSelectedObjects().isEmpty())
			return false;
		var parts = getSelectedObjects();
		for (Object o : parts) {
			if (!(o instanceof EditPart part))
				return false;
			if (!(part instanceof IOPaneEditPart paneEditPart))
				return false;
			if (paneEditPart.getModel().isForInputs() != forInput)
				return false;
		}
		return getCommand().canExecute();
	}

	@Override
	public void run() {
		execute(getCommand());
	}

	private Command getCommand() {
		var editparts = getSelectedObjects();
		CompoundCommand cc = new CompoundCommand();
		cc.setDebugLabel("Add " + (forInput ? "input" : "output") + " exchange");
		for (Object editpart : editparts) {
			var paneEditPart = (IOPaneEditPart) editpart;
			cc.add(paneEditPart.getCommand(request));
		}
		return cc.unwrap();

	}


}
