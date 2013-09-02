package org.openlca.app.editors.graphical.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.resources.ImageType;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class BuildSupplyChainMenuAction extends Action {

	private ProductSystemNode model;
	private ProcessDescriptor startProcess;
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

	void setModel(ProductSystemNode model) {
		this.model = model;
		systemBuild.setProductSystemNode(model);
		unitBuild.setProductSystemNode(model);
	}

	void setStartProcess(ProcessDescriptor startProcess) {
		this.startProcess = startProcess;
		systemBuild.setStartProcess(startProcess);
		unitBuild.setStartProcess(startProcess);
	}

	private class MenuCreator implements IMenuCreator {

		private Menu createMenu(Menu menu) {
			systemBuild.setProductSystemNode(model);
			systemBuild.setStartProcess(startProcess);
			MenuItem systemItem = new MenuItem(menu, SWT.NONE);
			systemItem.setText(systemBuild.getText());
			systemItem.addSelectionListener(new RunBuildListener(systemBuild));

			unitBuild.setProductSystemNode(model);
			unitBuild.setStartProcess(startProcess);
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
			action.run();
		}

	}

}
