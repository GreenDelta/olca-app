package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.resources.ImageType;
import org.openlca.core.model.ProcessType;

class BuildSupplyChainMenuAction extends EditorAction {

	private ProcessNode node;
	private BuildSupplyChainAction systemSupplyChainAction = new BuildSupplyChainAction(
			ProcessType.LCI_RESULT);
	private BuildSupplyChainAction unitSupplyChainAction = new BuildSupplyChainAction(
			ProcessType.UNIT_PROCESS);
	private BuildNextTierAction systemNextTierAction = new BuildNextTierAction(
			ProcessType.LCI_RESULT);
	private BuildNextTierAction unitNextTierAction = new BuildNextTierAction(
			ProcessType.UNIT_PROCESS);

	BuildSupplyChainMenuAction() {
		setId(ActionIds.BUILD_SUPPLY_CHAIN_MENU);
		setText(Messages.Systems_BuildSupplyChainAction_Text);
		setImageDescriptor(ImageType.BUILD_SUPPLY_CHAIN_ICON.getDescriptor());
		setMenuCreator(new MenuCreator());
	}

	private class MenuCreator implements IMenuCreator {

		private Menu createMenu(Menu menu) {
			MenuItem completeItem = new MenuItem(menu, SWT.CASCADE);
			completeItem.setText(Messages.Complete);
			Menu completeMenu = new Menu(completeItem);
			createItem(completeMenu, systemSupplyChainAction);
			createItem(completeMenu, unitSupplyChainAction);
			completeItem.setMenu(completeMenu);
			
			MenuItem nextTierItem = new MenuItem(menu, SWT.CASCADE);
			nextTierItem.setText(Messages.NextTier);
			Menu nextTierMenu = new Menu(nextTierItem);
			createItem(nextTierMenu, systemNextTierAction);
			createItem(nextTierMenu, unitNextTierAction);
			nextTierItem.setMenu(nextTierMenu);
			return menu;
		}

		private void createItem(Menu menu, IBuildAction action) {
			action.setProcessNode(node);
			MenuItem unitItem = new MenuItem(menu, SWT.NONE);
			unitItem.setText(action.getText());
			unitItem.addSelectionListener(new RunBuildListener(action));
		}

		@Override
		public void dispose() {
			// nothing to dispose
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

	private class RunBuildListener extends SelectionAdapter {

		private IBuildAction action;

		private RunBuildListener(IBuildAction action) {
			this.action = action;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			action.setProcessNode(node);
			action.run();
		}

	}

	@Override
	protected boolean accept(ISelection selection) {
		node = getSingleSelectionOfType(selection, ProcessNode.class);
		return node != null;
	}
}
