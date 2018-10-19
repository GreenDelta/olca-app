package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.command.CommandUtil;
import org.openlca.app.editors.graphical.command.ConnectionInput;
import org.openlca.app.editors.graphical.command.MassCreationCommand;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.editors.graphical.search.ConnectionDialog;
import org.openlca.app.util.Controls;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class SearchConnectorsAction extends EditorAction {

	static final int PROVIDER = 1;
	static final int RECIPIENTS = 2;
	private ProcessNode node;
	private int type;
	private Menu menu;

	public SearchConnectorsAction(int type) {
		super(type == PROVIDER ? M.SearchProvidersFor : M.SearchRecipientsFor, IAction.AS_DROP_DOWN_MENU);
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
		ProductSystemNode model = node.parent();
		long exchangeId = exchangeNode.exchange.getId();
		long flowId = exchangeNode.exchange.flow.getId();
		long nodeId = node.process.getId();
		boolean isWaste = exchangeNode.exchange.flow.getFlowType() == FlowType.WASTE_FLOW;
		ConnectionDialog dialog = new ConnectionDialog(exchangeNode);
		if (dialog.open() == IDialogConstants.OK_ID) {
			List<ProcessDescriptor> toCreate = dialog.toCreate();
			List<ConnectionInput> toConnect = new ArrayList<>();
			for (Pair<ProcessDescriptor, Long> next : dialog.toConnect())
				if ((type == PROVIDER && !isWaste) || (type == RECIPIENTS && isWaste))
					toConnect.add(new ConnectionInput(next.getLeft().getId(), flowId, nodeId, exchangeId, isWaste));
				else
					toConnect.add(new ConnectionInput(nodeId, flowId, next.getLeft().getId(), next.getRight(), isWaste));
			Command command = null;
			if (type == PROVIDER)
				command = MassCreationCommand.providers(toCreate, toConnect, model);
			else
				command = MassCreationCommand.recipients(toCreate, toConnect, model);
			CommandUtil.executeCommand(command, model.editor);
		}
	}

	@Override
	public void run() {
		// nothing to do (pop up menu)
	}

	private class MenuCreator implements IMenuCreator {

		private void fillMenu() {
			if (menu == null)
				return;
			for (MenuItem item : menu.getItems())
				item.dispose();
			boolean providers = type == PROVIDER;
			List<ExchangeNode> exchangeNodes = new ArrayList<>();
			for (ExchangeNode exchangeNode : node.loadExchangeNodes()) {
				if (exchangeNode.isDummy())
					continue;
				if (exchangeNode.exchange.isInput != providers)
					continue;
				exchangeNodes.add(exchangeNode);
			}
			for (ExchangeNode exchangeNode : exchangeNodes) {
				MenuItem item = new MenuItem(menu, SWT.NONE);
				item.setText(exchangeNode.getName());
				Controls.onSelect(item, (e) -> executeRequest(exchangeNode));
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
