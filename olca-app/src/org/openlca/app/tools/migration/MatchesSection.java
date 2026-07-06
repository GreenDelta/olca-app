package org.openlca.app.tools.migration;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.io.olca.migration.MigrationPlan;
import org.openlca.io.olca.migration.ProviderInfo;
import org.openlca.io.olca.migration.ProviderMatch;

final class MatchesSection {

	private final MigrationPlan plan;

	private MatchesSection(MigrationPlan plan) {
		this.plan = plan;
	}

	static void create(MigrationPlan plan, Composite parent, FormToolkit tk) {
		new MatchesSection(plan).render(parent, tk);
	}

	private void render(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Provider matches");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);

		var filterComp = UI.composite(comp, tk);
		UI.gridLayout(filterComp, 3);
		UI.stretchX(filterComp);
		UI.label(filterComp, tk, M.Filter);
		var searchText = UI.searchText(filterComp, tk);
		var strategyCombo = UI.combo(filterComp, tk);
		UI.gridData(strategyCombo, false, false);
		strategyCombo.setItems(MatchFilter.comboItems());
		strategyCombo.select(0);

		var table = Tables.createViewer(comp,
			"Source provider",
			"Target provider",
			"Status");
		table.setLabelProvider(new MatchLabel());
		table.setInput(Util.sortedMatches(plan.providerMatches()));
		Tables.bindColumnWidths2(table, 0.4, 0.4, 0.2);
		UI.stretchXY(table.getTable());

		new ModifySupport<ProviderMatch>(table)
			.bind("Target provider", new TargetProviderModifier());
		MatchFilter.on(table, searchText, strategyCombo);

		var edit = Actions.onEdit(() -> {
			ProviderMatch match = Viewers.getFirstSelected(table);
			if (match != null
				&& MatchSelectionDialog.open(match) == FormDialog.OK) {
				table.refresh();
			}
		});
		Actions.bind(table, edit);
		Tables.onDoubleClick(table, _ -> edit.run());

		var store = Actions.create(
			"Store provider matches",
			Icon.SAVE.descriptor(),
			() -> {
				var file = FileChooser.forSavingFile(
					"Store provider matches", "provider-matches.json");
				if (file == null)
					return;
				var res = MatchDump.store(plan, file);
				if (res.isError()) {
					MsgBox.error("Failed to store matches", res.error());
				}
			});

		var load = Actions.create(
			"Apply provider matches",
			Icon.IMPORT.descriptor(),
			() -> {
				var file = FileChooser.open("json");
				if (file == null)
					return;
				var res = MatchDump.apply(plan, file);
				if (res.isError()) {
					MsgBox.error("Failed to apply matches", res.error());
				} else {
					res.value().show();
					table.refresh();
				}
			});

		Actions.bind(section, store, load);
	}

	private static final class TargetProviderModifier
		extends ComboBoxCellModifier<ProviderMatch, ProviderInfo> {

		@Override
		protected ProviderInfo[] getItems(ProviderMatch row) {
			return row != null
				? row.alternatives().toArray(ProviderInfo[]::new)
				: new ProviderInfo[0];
		}

		@Override
		protected ProviderInfo getItem(ProviderMatch row) {
			return row != null ? row.selected() : null;
		}

		@Override
		protected String getText(ProviderInfo info) {
			return Util.labelOf(info);
		}

		@Override
		protected void setItem(ProviderMatch row, ProviderInfo item) {
			if (row == null || item == null || item == row.selected())
				return;
			row.select(item);
		}
	}

	static final class MatchLabel extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof ProviderMatch match))
				return null;
			if (col == 0 && match.source() != null)
				return Images.get(match.source().provider());
			if (col == 1 && match.selected() != null)
				return Images.get(match.selected().provider());
			return null;
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ProviderMatch match))
				return null;
			return switch (col) {
				case 0 -> match.source() != null
					? Labels.name(match.source().provider())
					: null;
				case 1 -> Util.labelOf(match.selected());
				case 2 -> statusOf(match);
				default -> null;
			};
		}

		private String statusOf(ProviderMatch match) {
			if (match == null)
				return null;
			var strategy = switch (match.strategy()) {
				case BY_ID -> "Exact ID";
				case BY_NAME -> "Name & location";
				case ANY -> "Any provider";
				case null -> "Manual selection";
			};
			int count = match.alternatives().size();
			return count == 1
				? strategy + " | 1 option"
				: strategy + " | " + count + " options";
		}
	}
}
