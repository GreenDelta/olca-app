package org.openlca.app.tools.migration;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.io.olca.migration.MatchingStrategy;

final class StrategyList {

	private final MigrationSetup config;
	private final Runnable onChange;
	private org.eclipse.swt.widgets.List list;
	private IconLink moveUpLink;
	private IconLink moveDownLink;
	private IconLink removeLink;

	private StrategyList(MigrationSetup config, Runnable onChange) {
		this.config = config;
		this.onChange = onChange;
	}

	static void create(
		Composite parent, FormToolkit tk, MigrationSetup config, Runnable onChange
	) {
		var strategyList = new StrategyList(config, onChange);
		strategyList.render(parent, tk);
	}

	private void render(Composite parent, FormToolkit tk) {
		UI.label(parent, tk, "Linking strategies");

		// the list with a column of icon links on the right
		var comp = UI.composite(parent, tk);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 2, 5, 0);
		list = new org.eclipse.swt.widgets.List(comp, SWT.BORDER | SWT.V_SCROLL);
		UI.gridData(list, true, true);

		var links = UI.composite(comp, tk);
		links.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		UI.gridLayout(links, 1, 5, 0);
		moveUpLink = new IconLink(links, tk, Icon.UP, Icon.UP_DISABLED,
			"Move up", () -> moveSelected(-1));
		moveDownLink = new IconLink(links, tk, Icon.DOWN, Icon.DOWN_DISABLED,
			"Move down", () -> moveSelected(1));
		removeLink = new IconLink(links, tk, Icon.DELETE, Icon.DELETE_DISABLED,
			"Remove", this::removeSelected);

		refresh();
		updateLinks();
		Controls.onSelect(list, _ -> {
			updateLinks();
			notifyChange();
		});

		// the same actions in the context menu
		var menu = new Menu(list);
		list.setMenu(menu);

		var moveUp = new MenuItem(menu, SWT.NONE);
		moveUp.setText("Move up");
		Controls.onSelect(moveUp, _ -> moveSelected(-1));

		var moveDown = new MenuItem(menu, SWT.NONE);
		moveDown.setText("Move down");
		Controls.onSelect(moveDown, _ -> moveSelected(1));

		new MenuItem(menu, SWT.SEPARATOR);

		var remove = new MenuItem(menu, SWT.NONE);
		remove.setText("Remove");
		Controls.onSelect(remove, _ -> removeSelected());

		menu.addListener(SWT.Show, _ -> {
			var strategy = selected();
			moveUp.setEnabled(config.canMoveUp(strategy));
			moveDown.setEnabled(config.canMoveDown(strategy));
			remove.setEnabled(strategy != null);
		});
	}

	/// Enables the links only for actions that are possible for the currently
	/// selected strategy.
	private void updateLinks() {
		if (moveUpLink == null)
			return;
		var strategy = selected();
		moveUpLink.setEnabled(config.canMoveUp(strategy));
		moveDownLink.setEnabled(config.canMoveDown(strategy));
		removeLink.setEnabled(strategy != null);
	}

	/// An image hyperlink without text that shows a separate icon when it is
	/// disabled. A disabled link does not receive mouse events and thus cannot
	/// be clicked.
	private static class IconLink {

		private final ImageHyperlink link;
		private final Icon icon;
		private final Icon disabledIcon;

		IconLink(
			Composite parent,
			FormToolkit tk,
			Icon icon,
			Icon disabledIcon,
			String tooltip,
			Runnable action
		) {
			this.icon = icon;
			this.disabledIcon = disabledIcon;
			link = tk != null
				? UI.imageHyperlink(parent, tk)
				: new ImageHyperlink(parent, SWT.NONE);
			link.setToolTipText(tooltip);
			Controls.onClick(link, _ -> action.run());
			setEnabled(false);
		}

		void setEnabled(boolean enabled) {
			if (link.isDisposed())
				return;
			link.setImage(enabled ? icon.get() : disabledIcon.get());
			link.setEnabled(enabled);
		}
	}

	private void moveSelected(int delta) {
		var strategy = selected();
		if (strategy == null)
			return;
		config.moveStrategy(strategy, delta);
		refresh();
		select(strategy);
		updateLinks();
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
		updateLinks();
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
