package org.openlca.app.editors.graphical.action;

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
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.core.matrix.LinkingConfig.DefaultProviders;
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
			createSubItem(subMenu, action, DefaultProviders.IGNORE, ProcessType.UNIT_PROCESS);
			createSubItem(subMenu, action, DefaultProviders.IGNORE, ProcessType.LCI_RESULT);
			createSubItem(subMenu, action, DefaultProviders.PREFER, ProcessType.UNIT_PROCESS);
			createSubItem(subMenu, action, DefaultProviders.PREFER, ProcessType.LCI_RESULT);
			createSubItem(subMenu, action, DefaultProviders.ONLY, null);
			item.setMenu(subMenu);
		}

		private void createSubItem(Menu menu, IBuildAction action, DefaultProviders providers, ProcessType type) {
			MenuItem item = new MenuItem(menu, SWT.NONE);
			String label = getLabel(providers);
			if (type != null) {
				label += "/" + NLS.bind(M.Prefer, Labels.of(type));
			}
			item.setText(label);
			Controls.onSelect(item, (e) -> {
				action.setProcessNodes(nodes);
				action.setProviderMethod(providers);
				action.setPreferredType(type);
				action.run();
			});
		}

		private String getLabel(DefaultProviders providers) {
			if (providers == null)
				return null;
			switch (providers) {
			case IGNORE:
				return M.IgnoreDefaultProviders;
			case PREFER:
				return M.PreferDefaultProviders;
			case ONLY:
				return M.OnlyLinkDefaultProviders;
			default:
				return null;
			}
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
