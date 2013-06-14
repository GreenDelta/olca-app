package org.openlca.core.editors.productsystem.graphical.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.Process;
import org.openlca.core.resources.ImageType;

/**
 * Drop down menu for the build supply chain actions
 * 
 * @author Sebastian Greve
 * 
 */
public class BuildSupplyChainMenuAction extends Action {

	/**
	 * The id of the action
	 */
	public static final String ID = "org.openlca.core.editors.productsystem.graphical.actions.BuildSupplyChainMenuAction";

	/**
	 * The product system node of the active editor
	 */
	private ProductSystemNode productSystemNode;

	/**
	 * The process to start building the supply chain from
	 */
	private Process startProcess;

	/**
	 * The build action with use of system processes
	 */
	private final BuildSupplyChainAction systemBuild = new BuildSupplyChainAction(
			true);

	/**
	 * The build action with use of unit processes
	 */
	private final BuildSupplyChainAction unitBuild = new BuildSupplyChainAction(
			false);

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.BUILD_SUPPLY_CHAIN_ICON.getDescriptor();
	}

	/**
	 * Creates a new instance
	 */
	public BuildSupplyChainMenuAction() {
		setId(ID);
		setMenuCreator(new MenuCreator());
	}

	@Override
	public String getText() {
		return Messages.Systems_BuildSupplyChainAction_Text;
	}

	/**
	 * Setter of the product system node
	 * 
	 * @param productSystemNode
	 *            The product system node
	 */
	public void setProductSystemNode(final ProductSystemNode productSystemNode) {
		this.productSystemNode = productSystemNode;
		systemBuild.setProductSystemNode(productSystemNode);
		unitBuild.setProductSystemNode(productSystemNode);
	}

	/**
	 * Setter of the start process
	 * 
	 * @param startProcess
	 *            The process to start building the supply chain from
	 */
	public void setStartProcess(final Process startProcess) {
		this.startProcess = startProcess;
		systemBuild.setStartProcess(startProcess);
		unitBuild.setStartProcess(startProcess);
	}

	/**
	 * The menu creator
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class MenuCreator implements IMenuCreator {

		/**
		 * Creates the menu
		 * 
		 * @param menu
		 *            The parent menu
		 * @return The parent menu
		 */
		private Menu createMenu(final Menu menu) {
			systemBuild.setProductSystemNode(productSystemNode);
			systemBuild.setStartProcess(startProcess);
			final MenuItem item = new MenuItem(menu, SWT.NONE);
			item.setText(systemBuild.getText());
			item.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					// no action on default selection
				}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					systemBuild.run();
				}

			});

			unitBuild.setProductSystemNode(productSystemNode);
			unitBuild.setStartProcess(startProcess);
			final MenuItem item2 = new MenuItem(menu, SWT.NONE);
			item2.setText(unitBuild.getText());
			item2.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					// no action on default selection
				}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					unitBuild.run();
				}

			});
			return menu;
		}

		@Override
		public void dispose() {
			// nothing to dispose
		}

		@Override
		public Menu getMenu(final Control control) {
			final Menu menu = new Menu(control);
			createMenu(menu);
			control.setMenu(menu);
			return menu;
		}

		@Override
		public Menu getMenu(final Menu parent) {
			final Menu menu = new Menu(parent);
			createMenu(menu);
			return menu;
		}

	}
}
