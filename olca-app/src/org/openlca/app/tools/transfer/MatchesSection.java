package org.openlca.app.tools.transfer;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.io.olca.systransfer.ProviderInfo;
import org.openlca.io.olca.systransfer.ProviderMatch;
import org.openlca.io.olca.systransfer.TransferPlan;

final class MatchesSection {

	private MatchesSection() {
	}

	static void create(Composite parent, FormToolkit tk, TransferPlan plan) {
		var section = UI.section(parent, tk, "Provider matches");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var matchesTable = Tables.createViewer(comp,
			"Provider",
			"Selected provider",
			"Status");
		matchesTable.setLabelProvider(new MatchLabel());
		matchesTable.setInput(plan.matches());
		Tables.bindColumnWidths2(matchesTable, 0.42, 0.42, 0.16);
		var table = matchesTable.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		UI.gridData(table, true, true);

		new ModifySupport<ProviderMatch>(matchesTable)
			.bind("Selected provider", new SelectedProviderModifier());
	}

	static final class SelectedProviderModifier
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
		protected String getText(ProviderInfo value) {
			return TransferProviderLabels.of(value);
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
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ProviderMatch match))
				return null;
			return switch (columnIndex) {
				case 0 -> TransferProviderLabels.of(match.provider());
				case 1 -> TransferProviderLabels.of(match.selected());
				case 2 -> statusOf(match);
				default -> null;
			};
		}

		private String statusOf(ProviderMatch match) {
			int count = match.alternatives().size();
			return count == 1 ? "Single candidate" : count + " candidates";
		}
	}
}
