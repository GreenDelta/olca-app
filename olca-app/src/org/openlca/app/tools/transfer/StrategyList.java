package org.openlca.app.tools.transfer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.io.olca.systransfer.MatchingStrategy;

final class StrategyList {

	private final TransferSetup config;
	private final Runnable onChange;
	private org.eclipse.swt.widgets.List list;

	private StrategyList(TransferSetup config, Runnable onChange) {
		this.config = config;
		this.onChange = onChange;
	}

	static void create(
		Composite parent, FormToolkit tk, TransferSetup config, Runnable onChange
	) {
		var strategyList = new StrategyList(config, onChange);
		strategyList.render(parent, tk);
	}

	private void render(Composite parent, FormToolkit tk) {
		UI.label(parent, tk, "Linking strategies");
		list = new org.eclipse.swt.widgets.List(parent, SWT.BORDER | SWT.V_SCROLL);
		UI.gridData(list, true, true);
		refresh();
		Controls.onSelect(list, $ -> notifyChange());

		var menu = new Menu(list);
		list.setMenu(menu);

		var moveUp = new MenuItem(menu, SWT.NONE);
		moveUp.setText("Move up");
		Controls.onSelect(moveUp, $ -> moveSelected(-1));

		var moveDown = new MenuItem(menu, SWT.NONE);
		moveDown.setText("Move down");
		Controls.onSelect(moveDown, $ -> moveSelected(1));

		new MenuItem(menu, SWT.SEPARATOR);

		var remove = new MenuItem(menu, SWT.NONE);
		remove.setText("Remove");
		Controls.onSelect(remove, $ -> removeSelected());

		menu.addListener(SWT.Show, $ -> {
			var strategy = selected();
			moveUp.setEnabled(config.canMoveUp(strategy));
			moveDown.setEnabled(config.canMoveDown(strategy));
			remove.setEnabled(strategy != null);
		});
	}

	private void moveSelected(int delta) {
		var strategy = selected();
		if (strategy == null)
			return;
		config.moveStrategy(strategy, delta);
		refresh();
		select(strategy);
		notifyChange();
	}

	private void removeSelected() {
		var strategy = selected();
		if (strategy == null)
			return;
		int index = config.strategies().indexOf(strategy);
		config.removeStrategy(strategy);
		refresh();
		if (!config.strategies().isEmpty()) {
			int nextIndex = Math.min(index, config.strategies().size() - 1);
			list.setSelection(nextIndex);
		} else {
			list.deselectAll();
		}
		notifyChange();
	}

	private MatchingStrategy selected() {
		if (list == null || list.isDisposed())
			return null;
		int index = list.getSelectionIndex();
		return index >= 0 && index < config.strategies().size()
			? config.strategies().get(index)
			: null;
	}

	private void refresh() {
		if (list == null || list.isDisposed())
			return;
		list.removeAll();
		for (var strategy : config.strategies()) {
			list.add(labelOf(strategy));
		}
	}

	private void select(MatchingStrategy strategy) {
		if (strategy == null || list == null || list.isDisposed())
			return;
		int index = config.strategies().indexOf(strategy);
		if (index >= 0) {
			list.setSelection(index);
		}
	}

	private void notifyChange() {
		if (onChange != null) {
			onChange.run();
		}
	}

	static String labelOf(MatchingStrategy strategy) {
		return switch (strategy) {
			case BY_ID -> "Match by flow and exact provider ID";
			case BY_NAME -> "Match by flow, provider name, and location";
			case ANY -> "Match by flow only (take any available provider)";
		};
	}
}
