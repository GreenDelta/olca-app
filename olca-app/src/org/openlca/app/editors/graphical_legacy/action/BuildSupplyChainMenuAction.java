package org.openlca.app.editors.graphical_legacy.action;

import java.util.List;

import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.ProcessType;

public class BuildSupplyChainMenuAction extends Action implements UpdateAction {

	private final GraphEditor editor;
	private List<ProcessNode> nodes;

	public BuildSupplyChainMenuAction(GraphEditor editor) {
		this.editor = editor;
		setId(ActionIds.BUILD_SUPPLY_CHAIN_MENU);
		setText(M.BuildSupplyChain);
		setImageDescriptor(Icon.BUILD_SUPPLY_CHAIN.descriptor());
		setMenuCreator(new MenuCreator());
	}

	private class MenuCreator implements IMenuCreator {

		private void createMenu(Menu menu) {
			createItem(menu, new BuildSupplyChainAction());
			createItem(menu, new BuildNextTierAction());
		}

		private void createItem(Menu menu, IBuildAction action) {
			var item = new MenuItem(menu, SWT.CASCADE);
			item.setText(action.getText());
			var subMenu = new Menu(item);
			createSubItem(subMenu, action, ProviderLinking.IGNORE_DEFAULTS, ProcessType.UNIT_PROCESS);
			createSubItem(subMenu, action, ProviderLinking.IGNORE_DEFAULTS, ProcessType.LCI_RESULT);
			createSubItem(subMenu, action, ProviderLinking.PREFER_DEFAULTS, ProcessType.UNIT_PROCESS);
			createSubItem(subMenu, action, ProviderLinking.PREFER_DEFAULTS, ProcessType.LCI_RESULT);
			createSubItem(subMenu, action, ProviderLinking.ONLY_DEFAULTS, null);
			item.setMenu(subMenu);
		}

		private void createSubItem(
				Menu menu, IBuildAction action, ProviderLinking linking, ProcessType type) {
			MenuItem item = new MenuItem(menu, SWT.NONE);
			String label = Labels.of(linking);
			if (type != null) {
				label += "/" + NLS.bind(M.Prefer, Labels.of(type));
			}
			item.setText(label);
			Controls.onSelect(item, (e) -> {
				action.setProcessNodes(nodes);
				action.setProviderMethod(linking);
				action.setPreferredType(type);
				action.run();
			});
		}

		@Override
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
	public void update() {
		nodes = GraphActions.allSelectedOf(editor, ProcessNode.class);
		setEnabled(!nodes.isEmpty());
	}
}
