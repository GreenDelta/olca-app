package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.ProcessType;

import java.util.ArrayList;
import java.util.List;

public class BuildSupplyChainMenuAction extends SelectionAction implements UpdateAction {

	private List<Node> nodes;

	public BuildSupplyChainMenuAction(GraphEditor editor) {
		super(editor);
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
	protected boolean calculateEnabled() {
		if (getSelectedObjects().isEmpty())
			return false;

		nodes = new ArrayList<>();
		for (var object : getSelectedObjects()) {
			if (NodeEditPart.class.isAssignableFrom(object.getClass())) {
				nodes.add(((NodeEditPart) object).getModel());
			}
		}
		return !nodes.isEmpty();
	}

}
