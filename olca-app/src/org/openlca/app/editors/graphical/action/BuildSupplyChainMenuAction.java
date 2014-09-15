package org.openlca.app.editors.graphical.action;

import java.util.List;

import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.core.model.ProcessType;

class BuildSupplyChainMenuAction extends EditorAction {

	private List<ProcessNode> nodes;
	private BuildSupplyChainAction supplyChainAction = ActionFactory
			.createBuildSupplyChainAction();
	private BuildNextTierAction nextTierAction = ActionFactory
			.createBuildNextTierAction();

	BuildSupplyChainMenuAction() {
		setId(ActionIds.BUILD_SUPPLY_CHAIN_MENU);
		setText(Messages.BuildSupplyChain);
		setImageDescriptor(ImageType.BUILD_SUPPLY_CHAIN_ICON.getDescriptor());
		setMenuCreator(new MenuCreator());
	}

	private class MenuCreator implements IMenuCreator {

		private Menu createMenu(Menu menu) {
			createItem(menu, supplyChainAction);
			createItem(menu, nextTierAction);
			new MenuItem(menu, SWT.SEPARATOR);
			createSelectTypeItem(menu, ProcessType.UNIT_PROCESS);
			createSelectTypeItem(menu, ProcessType.LCI_RESULT);
			return menu;
		}

		private void createSelectTypeItem(Menu menu, final ProcessType type) {
			MenuItem treeItem = new MenuItem(menu, SWT.RADIO);
			treeItem.setText(Messages.bind(Messages.Prefer,
					getDisplayName(type)));
			Controls.onSelect(treeItem, (e) -> {
				supplyChainAction.setPreferredType(type);
				nextTierAction.setPreferredType(type);
			});
			treeItem.setSelection(type == ProcessType.UNIT_PROCESS);
		}

		private String getDisplayName(ProcessType type) {
			switch (type) {
			case UNIT_PROCESS:
				return Messages.UnitProcess;
			case LCI_RESULT:
				return Messages.SystemProcess;
			}
			return "";
		}

		private void createItem(Menu menu, IBuildAction action) {
			MenuItem item = new MenuItem(menu, SWT.NONE);
			item.setText(action.getText());
			Controls.onSelect(item, (e) -> {
				action.setProcessNodes(nodes);
				action.run();
			});
		}

		public void dispose() {
		}

		@Override
		public Menu getMenu(Control control) {
			Menu menu = new Menu(control);
			createMenu(menu);
			control.setMenu(menu);
			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			Menu menu = new Menu(parent);
			createMenu(menu);
			return menu;
		}

	}

	@Override
	protected boolean accept(ISelection selection) {
		nodes = getMultiSelectionOfType(selection, ProcessNode.class);
		return nodes != null && !nodes.isEmpty();
	}
}
