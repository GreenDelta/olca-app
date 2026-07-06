package org.openlca.app.tools.migration;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.commons.Strings;
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
	}

	/// We cannot use `Labels.name` for the target provider, as `Labels.name`
	/// may look into the current database for location suffices etc.
	private static String labelOf(ProviderInfo info) {
		if (info == null)
			return null;
		var label = info.provider() != null
			? info.provider().name
			: null;
		if (label == null)
			return "-";
		return info.location() != null && Strings.isNotBlank(info.location().code)
			? label + " - " + info.location().code
			: label;
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
			return labelOf(info);
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
				case 1 -> labelOf(match.selected());
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
