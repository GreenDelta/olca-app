package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.ConnectorDialog;
import org.openlca.app.editors.graphical.command.CommandFactory;
import org.openlca.app.editors.graphical.command.CommandUtil;
import org.openlca.app.editors.graphical.command.ConnectionInput;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.util.Controls;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class SearchConnectorsAction extends EditorAction {

	static final int PROVIDER = 1;
	static final int RECIPIENTS = 2;
	private ProcessNode node;
	private int type;
	private Menu menu;

	public SearchConnectorsAction(int type) {
		super(type == PROVIDER ? Messages.SearchProvidersFor
				: Messages.SearchRecipientsFor,
				IAction.AS_DROP_DOWN_MENU);
		if (type == PROVIDER)
			setId(ActionIds.SEARCH_PROVIDERS);
		else if (type == RECIPIENTS)
			setId(ActionIds.SEARCH_RECIPIENTS);
		this.type = type;
		setMenuCreator(new MenuCreator());
	}

	@Override
	protected boolean accept(ISelection selection) {
		node = getSingleSelectionOfType(selection, ProcessNode.class);
		if (node != null)
			((MenuCreator) getMenuCreator()).fillMenu();
		return node != null;
	}

	private void executeRequest(ExchangeNode exchangeNode) {
		ProductSystemNode model = node.getParent();
		long flowId = exchangeNode.getExchange().getFlow().getId();
		long nodeId = node.getProcess().getId();
		ConnectorDialog dialog = new ConnectorDialog(exchangeNode);
		if (dialog.open() == IDialogConstants.OK_ID) {
			List<ProcessDescriptor> toCreate = dialog.getProcessesToCreate();
			List<ConnectionInput> toConnect = new ArrayList<>();
			for (ProcessDescriptor process : dialog.getProcessesToConnect())
				if (type == PROVIDER)
					toConnect.add(new ConnectionInput(process.getId(), nodeId,
							flowId));
				else if (type == RECIPIENTS)
					toConnect.add(new ConnectionInput(nodeId, process.getId(),
							flowId));
			Command command = null;
			if (type == PROVIDER)
				command = CommandFactory.createConnectProvidersCommand(
						toCreate, toConnect, model);
			else if (type == RECIPIENTS)
				command = CommandFactory.createConnectRecipientsCommand(
						toCreate, toConnect, model);
			CommandUtil.executeCommand(command, model.getEditor());
		}
	}

	@Override
	public void run() {
		// nothing to do (pop up menu)
	}

	private class MenuCreator implements IMenuCreator {

		private void fillMenu() {
			if (menu != null) {
				for (MenuItem item : menu.getItems())
					item.dispose();

				boolean providers = type == PROVIDER;
				List<ExchangeNode> exchangeNodes = new ArrayList<>();
				for (ExchangeNode exchangeNode : node.loadExchangeNodes()) {
					if (exchangeNode.isDummy())
						continue;
					if (exchangeNode.getExchange().isInput() == providers)
						exchangeNodes.add(exchangeNode);
				}

				for (final ExchangeNode exchangeNode : exchangeNodes) {
					MenuItem item = new MenuItem(menu, SWT.NONE);
					item.setText(exchangeNode.getName());
					Controls.onSelect(item, (e) -> executeRequest(exchangeNode));
				}
			}
		}

		@Override
		public void dispose() {
			// nothing to dispose
		}

		@Override
		public Menu getMenu(Control control) {
			menu = new Menu(control);
			fillMenu();
			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			menu = new Menu(parent);
			fillMenu();
			return menu;
		}
	}
}
