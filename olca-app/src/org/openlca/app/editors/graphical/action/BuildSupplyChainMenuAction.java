package org.openlca.app.editors.graphical.action;

import org.eclipse.gef.EditPart;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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

public class BuildSupplyChainMenuAction extends EditorAction {

	private ProcessNode node;
	private BuildSupplyChainAction systemBuild = new BuildSupplyChainAction(
			ProcessType.LCI_RESULT);
	private BuildSupplyChainAction unitBuild = new BuildSupplyChainAction(
			ProcessType.UNIT_PROCESS);

	public BuildSupplyChainMenuAction() {
		setId(ActionIds.BUILD_SUPPLY_CHAIN_MENU_ACTION_ID);
		setText(Messages.Systems_BuildSupplyChainAction_Text);
		setImageDescriptor(ImageType.BUILD_SUPPLY_CHAIN_ICON.getDescriptor());
		setMenuCreator(new MenuCreator());
	}

	private class MenuCreator implements IMenuCreator {

		private Menu createMenu(Menu menu) {
			systemBuild.setNode(node);
			MenuItem systemItem = new MenuItem(menu, SWT.NONE);
			systemItem.setText(systemBuild.getText());
			systemItem.addSelectionListener(new RunBuildListener(systemBuild));

			unitBuild.setNode(node);
			MenuItem unitItem = new MenuItem(menu, SWT.NONE);
			unitItem.setText(unitBuild.getText());
			unitItem.addSelectionListener(new RunBuildListener(unitBuild));
			return menu;
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

		private BuildSupplyChainAction action;

		private RunBuildListener(BuildSupplyChainAction action) {
			this.action = action;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			action.setNode(node);
			action.run();
		}

	}

	@Override
	protected boolean accept(ISelection selection) {
		node = null;
		if (selection == null)
			return false;
		if (selection.isEmpty())
			return false;
		if (!(selection instanceof IStructuredSelection))
			return false;

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() > 1)
			return false;
		if (!(sel.getFirstElement() instanceof EditPart))
			return false;
		Object model = ((EditPart) sel.getFirstElement()).getModel();
		if (!(model instanceof ProcessNode))
			return false;
		node = (ProcessNode) model;
		return true;
	}
}
