package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.IOPane;
import org.openlca.app.editors.graphical.model.commands.MassCreationCommand;
import org.openlca.app.editors.graphical.search.ConnectionDialog;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.List;

public class SearchConnectorsAction extends SelectionAction {

	public static final int PROVIDER = 1;
	public static final int RECIPIENTS = 2;
	private final int type;
	private IOPane ioPane;

	public SearchConnectorsAction(GraphEditor part, int type) {
		super(part);
		if (type == PROVIDER) {
			setId(GraphActionIds.SEARCH_PROVIDERS);
			setText(M.SearchProvidersFor);
		}
		else if (type == RECIPIENTS) {
			setId(GraphActionIds.SEARCH_RECIPIENTS);
			setText(M.SearchRecipientsFor);
		}
		this.type = type;
		setMenuCreator(new MenuCreator());
	}

	@Override
	protected boolean calculateEnabled() {
		if (getSelectedObjects().size() != 1)
			return false;

		var object = getSelectedObjects().get(0);
		// The selected object is a node.
		if (NodeEditPart.class.isAssignableFrom(object.getClass())) {
			var node = ((NodeEditPart) object).getModel();
			if (node != null) {
				ioPane = type == PROVIDER
						? node.getInputIOPane()
						: node.getOutputIOPane();
				if (ioPane != null &&
						(type == RECIPIENTS || !ioPane.hasOnlyElementary())) {
					((MenuCreator) getMenuCreator()).fillMenu();
					return true;
				}
			}
		}
		return false;
	}

	private void executeRequest(ExchangeItem exchangeItem) {
		var graph = exchangeItem.getGraph();
		ConnectionDialog dialog = new ConnectionDialog(exchangeItem);
		if (dialog.open() != IDialogConstants.OK_ID)
			return;
		List<RootDescriptor> newProcesses = dialog.getNewProcesses();
		List<ProcessLink> newLinks = dialog.getNewLinks();
		Command command = type == PROVIDER
				? MassCreationCommand.providers(newProcesses, newLinks, graph)
				: MassCreationCommand.recipients(newProcesses, newLinks, graph);
		execute(command);
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
			for (var n : ioPane.getExchangesItems()) {
				if ((n.exchange.isInput != providers)
						|| (providers && n.isElementary()))
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
