package org.openlca.app.tools.transfer;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.io.olca.systransfer.ProviderInfo;
import org.openlca.io.olca.systransfer.TransferPlan;

final class TransferPlanPage extends FormPage {

	private final TransferPlanEditor editor;
	private final TransferPlan plan;

	TransferPlanPage(TransferPlanEditor editor) {
		super(editor, "TransferPlanEditor.Page", "Transfer plan");
		this.editor = editor;
		this.plan = editor.plan();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "Transfer plan", Icon.TARGET.get());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		createSummary(body, tk);
		createMetaSection(body, tk);
		createCopiesSection(body, tk);
		MatchesSection.create(plan, body, tk);

		form.reflow(true);
	}

	private void createSummary(Composite parent, FormToolkit tk) {
		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 2);
		UI.gridData(comp, true, false);

		var summary = UI.label(comp, tk, summaryText());
		UI.gridData(summary, true, false);

		var button = UI.button(comp, tk, "Transfer");
		button.setImage(Icon.RUN.get());
		Controls.onSelect(button, $ -> editor.runTransfer());
	}

	private void createMetaSection(Composite parent, FormToolkit tk) {
		var comp = UI.formSection(parent, tk, "Transfer metadata", 2);
		var config = plan.config();

		UI.label(comp, tk, "Source database");
		UI.label(comp, tk, config.source().getName());

		UI.label(comp, tk, "Target database");
		UI.label(comp, tk, config.target().getName());

		UI.label(comp, tk, "Product system");
		UI.label(comp, tk, config.system() != null ? config.system().name : null);

		UI.label(comp, tk, "Matching strategies");
		UI.label(comp, tk, strategiesText());
	}

	private void createCopiesSection(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Providers copied to target database");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var copiedTable = Tables.createViewer(comp, "Provider", "Flow", "Location");
		copiedTable.setLabelProvider(new ProviderInfoLabel());
		copiedTable.setInput(plan.copies());
		Tables.bindColumnWidths2(copiedTable, 0.5, 0.3, 0.2);
		var table = copiedTable.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		UI.gridData(table, true, true);
	}

	private String summaryText() {
		return plan.matches().size() + " provider match"
			+ (plan.matches().size() == 1 ? "" : "es")
			+ " found; " + plan.copies().size() + " provider"
			+ (plan.copies().size() == 1 ? "" : "s")
			+ " will be copied.";
	}

	private String strategiesText() {
		var strategies = plan.config().strategies();
		if (strategies == null || strategies.length == 0)
			return "None";
		var text = new StringBuilder();
		for (int i = 0; i < strategies.length; i++) {
			if (i > 0) {
				text.append(" -> ");
			}
			text.append(i + 1).append(". ").append(strategies[i]);
		}
		return text.toString();
	}

	private static final class ProviderInfoLabel extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return columnIndex == 0 ? Icon.LINK.get() : null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ProviderInfo info))
				return null;
			return switch (columnIndex) {
				case 0 -> TransferProviderLabels.providerOnly(info);
				case 1 -> info.flow() != null ? info.flow().name : null;
				case 2 -> info.location() != null ? info.location().code : null;
				default -> null;
			};
		}
	}
}
