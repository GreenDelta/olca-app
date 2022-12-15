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
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;

import java.util.HashMap;
import java.util.Map;

public class BuildSupplyChainMenuAction extends SelectionAction
		implements UpdateAction {

	private final GraphEditor editor;
	protected Map<Exchange, Process> exchanges;

	public BuildSupplyChainMenuAction(GraphEditor part) {
		super(part);
		editor = part;
		setId(GraphActionIds.BUILD_SUPPLY_CHAIN_MENU);
		setImageDescriptor(Icon.BUILD_SUPPLY_CHAIN.descriptor());
		setMenuCreator(new MenuCreator());
	}

	private class MenuCreator implements IMenuCreator {

		private void createMenu(Menu menu) {
			createItem(menu, new BuildSupplyChainAction(editor));
			createItem(menu, new BuildNextTierAction(editor));
		}

		private void createItem(Menu menu, BuildAction action) {
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
			Menu menu, BuildAction action, ProviderLinking linking, ProcessType type) {
			MenuItem item = new MenuItem(menu, SWT.NONE);
			String label = Labels.of(linking);
			if (type != null) {
				label += "/" + NLS.bind(M.Prefer, Labels.of(type));
			}
			item.setText(label);
			Controls.onSelect(item, (e) -> {
				action.setMapExchangeToProcess(exchanges);
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

		exchanges = new HashMap<>();
		for (var object : getSelectedObjects()) {
			if (NodeEditPart.class.isAssignableFrom(object.getClass())) {
				setText(M.BuildSupplyChain);
				var node = ((NodeEditPart) object).getModel();
				if (node.getEntity() instanceof Process process)
					for (var e : process.exchanges)
						if (isCandidate(e))
							exchanges.put(e, process);
			}
			else if (object instanceof ExchangeEditPart part) {
				setText(M.BuildFlowSupplyChain);
				if (part.getModel().getNode().getEntity() instanceof Process process
						&& isCandidate(part.getModel().exchange)) {
					exchanges.put(part.getModel().exchange, process);
				}
			}
		}
		return !exchanges.isEmpty();
	}

	private boolean isCandidate(Exchange e) {
		return (e.flow.flowType == FlowType.WASTE_FLOW && !e.isInput)
				|| (e.flow.flowType == FlowType.PRODUCT_FLOW && e.isInput);
	}

}
