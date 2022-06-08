package org.openlca.app.editors.graphical_legacy.action;

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
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.command.Commands;
import org.openlca.app.editors.graphical_legacy.command.MassCreationCommand;
import org.openlca.app.editors.graphical_legacy.model.ExchangeNode;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;
import org.openlca.app.editors.graphical_legacy.search.ConnectionDialog;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.RootDescriptor;

class SearchConnectorsAction extends EditorAction {

	static final int PROVIDER = 1;
	static final int RECIPIENTS = 2;
	private final int type;
	private ProcessNode node;

	public SearchConnectorsAction(int type) {
		super(type == PROVIDER
				? M.SearchProvidersFor
				: M.SearchRecipientsFor,
				IAction.AS_DROP_DOWN_MENU);
		if (type == PROVIDER)
			setId(ActionIds.SEARCH_PROVIDERS);
		else if (type == RECIPIENTS)
			setId(ActionIds.SEARCH_RECIPIENTS);
		this.type = type;
		setMenuCreator(new MenuCreator());
	}

	@Override
	protected boolean accept(ISelection s) {
		node = GraphActions.firstSelectedOf(s, ProcessNode.class);
		if (node != null)
			((MenuCreator) getMenuCreator()).fillMenu();
		return node != null;
	}

	private void executeRequest(ExchangeNode enode) {
		ProductSystemNode sysNode = node.parent();
		ConnectionDialog dialog = new ConnectionDialog(enode);
		if (dialog.open() != IDialogConstants.OK_ID)
			return;
		List<RootDescriptor> newProcesses = dialog.getNewProcesses();
		List<ProcessLink> newLinks = dialog.getNewLinks();
		Command command = type == PROVIDER
				? MassCreationCommand.providers(newProcesses, newLinks, sysNode)
				: MassCreationCommand.recipients(newProcesses, newLinks, sysNode);
		Commands.executeCommand(command, sysNode.editor);
	}

	@Override
	public void run() {
	}

	private class MenuCreator implements IMenuCreator {

		private Menu menu;

		private void fillMenu() {
			if (menu == null)
				return;
			for (var item : menu.getItems()) {
				item.dispose();
			}
			boolean providers = type == PROVIDER;
			for (var n : node.loadExchangeNodes()) {
				if (n.exchange.isInput != providers)
					continue;
				var label = Labels.name(n.exchange.flow);
				var item = new MenuItem(menu, SWT.NONE);
				item.setText(label);
				Controls.onSelect(item, $ -> executeRequest(n));
			}
		}

		@Override
		public void dispose() {
			if (menu != null && !menu.isDisposed()) {
				menu.dispose();
				menu = null;
			}
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
