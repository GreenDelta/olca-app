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

	private final TransferPlan plan;

	private MatchesSection(TransferPlan plan) {
		this.plan = plan;
	}

	static void create(TransferPlan plan, Composite parent, FormToolkit tk) {
		new MatchesSection(plan).render(parent, tk);
	}

	private void render(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Provider matches");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(comp,
			"Source provider",
			"Flow",
			"Target provider",
			"Status");
		table.setLabelProvider(new MatchLabel());
		table.setInput(plan.matches());
		Tables.bindColumnWidths2(table, 0.25, 0.25, 0.25, 0.25);
		var gd = UI.gridData(table.getTable(), true, true);
		gd.heightHint = 1;
		gd.widthHint = 1;

		new ModifySupport<ProviderMatch>(table)
			.bind("Target provider", new TargetProviderModifier());
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
