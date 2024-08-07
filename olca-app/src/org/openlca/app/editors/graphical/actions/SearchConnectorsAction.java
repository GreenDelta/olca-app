package org.openlca.app.editors.graphical.actions;

import java.util.List;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.commands.MassCreationCommand;
import org.openlca.app.editors.graphical.search.ConnectionDialog;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.RootDescriptor;

public class SearchConnectorsAction extends SelectionAction {

	public static final int PROVIDER = 1;
	public static final int RECIPIENTS = 2;
	private final int type;
	private ExchangeItem exchangeItem;

	public SearchConnectorsAction(GraphEditor part, int type) {
		super(part);
		this.type = type;

		if (type == PROVIDER) {
			setId(GraphActionIds.SEARCH_PROVIDERS);
			setText(M.SearchProviders);
		}
		else if (type == RECIPIENTS) {
			setId(GraphActionIds.SEARCH_RECIPIENTS);
			setText(M.SearchRecipients);
		}
		setImageDescriptor(Icon.PROCESS_ADD.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		var objects = getSelectedObjects();
		if (objects.size() != 1)
			return false;

		var object = objects.get(0);
		if (object instanceof ExchangeEditPart part) {
			if (type == PROVIDER && part.getModel().isElementary())
				return false;
			else {
				exchangeItem = part.getModel();
				return true;
			}
		}
		else return false;
	}

	@Override
	public void run() {
		if (exchangeItem == null)
			return;

		var graph = exchangeItem.getGraph();
		var isDirty = graph.getEditor().isDirty(exchangeItem.getNode().getEntity());
		var dialog = new ConnectionDialog(exchangeItem, isDirty);
		if (dialog.open() != IDialogConstants.OK_ID)
			return;

		List<RootDescriptor> newProcesses = dialog.getNewProcesses();
		List<ProcessLink> newLinks = dialog.getNewLinks();
		var command = type == PROVIDER
				? MassCreationCommand.providers(newProcesses, newLinks, graph)
				: MassCreationCommand.recipients(newProcesses, newLinks, graph);
		if (command.canExecute())
			execute(command);
	}

}
