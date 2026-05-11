package org.openlca.app.tools.transfer;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.commons.Res;
import org.openlca.io.olca.systransfer.ProviderInfo;
import org.openlca.io.olca.systransfer.ProviderMatch;
import org.openlca.io.olca.systransfer.TransferExecutor;
import org.openlca.io.olca.systransfer.TransferPlan;

public class TransferPlanEditor extends SimpleFormEditor {

	private static final String ID = "TransferPlanEditor";

	private TransferPlan plan;
	private boolean running;

	public static void open(TransferPlan plan) {
		if (plan == null)	return;
		var key = AppContext.put(plan);
		var input = new SimpleEditorInput(key, titleOf(plan));
		Editors.open(input, ID);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setTitleImage(Icon.TARGET.get());
		if (!(input instanceof SimpleEditorInput editorInput)) {
			throw new PartInitException("No transfer plan provided");
		}
		plan = AppContext.remove(editorInput.id, TransferPlan.class);
		if (plan == null) {
			throw new PartInitException("The transfer plan is no longer available");
		}
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private void runTransfer() {
		if (running || plan == null)
			return;
		running = true;
		var execRes = new Res[1];
		App.runWithProgress("Transfer product system", () ->
			execRes[0] = TransferExecutor.of(plan).execute());
		running = false;
		if (execRes[0] == null || execRes[0].isError()) {
			var error = execRes[0] != null
				? execRes[0].error()
				: "Failed to transfer the product system";
			MsgBox.error("Transfer failed", error);
			return;
		}

		var config = plan.config();
		MsgBox.info("Transfer complete",
			"Transferred product system to target database '"
				+ config.target().getName() + "' with "
				+ plan.matches().size() + " provider assignment"
				+ (plan.matches().size() == 1 ? "" : "s")
				+ " and " + plan.copies().size() + " provider "
				+ (plan.copies().size() == 1 ? "copy" : "copies") + ".");
	}

	private static String titleOf(TransferPlan plan) {
		var config = plan.config();
		var system = config != null && config.system() != null && config.system().name != null
			? config.system().name
			: "Product system";
		return "Transfer plan - " + system;
	}

	private static final class Page extends FormPage {

		private final TransferPlanEditor editor;

		Page(TransferPlanEditor editor) {
			super(editor, ID + ".Page", "Transfer plan");
			this.editor = editor;
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.header(mForm, "Transfer plan", Icon.TARGET.get());
			var tk = mForm.getToolkit();
			var body = UI.body(form, tk);

			createSummary(body, tk);
			createMetaSection(body, tk);
			createCopiesSection(body, tk);
			createMatchesSection(body, tk);

			form.reflow(true);
		}

		private void createSummary(Composite parent, org.eclipse.ui.forms.widgets.FormToolkit tk) {
			var comp = UI.composite(parent, tk);
			UI.gridLayout(comp, 2);
			UI.gridData(comp, true, false);

			var summary = UI.label(comp, tk, summaryText());
			UI.gridData(summary, true, false);

			var button = UI.button(comp, tk, "Transfer");
			button.setImage(Icon.RUN.get());
			Controls.onSelect(button, $ -> editor.runTransfer());
		}

		private void createMetaSection(Composite parent, org.eclipse.ui.forms.widgets.FormToolkit tk) {
			var comp = UI.formSection(parent, tk, "Transfer metadata", 2);
			var config = editor.plan.config();

			UI.label(comp, tk, "Source database");
			UI.label(comp, tk, config.source().getName());

			UI.label(comp, tk, "Target database");
			UI.label(comp, tk, config.target().getName());

			UI.label(comp, tk, "Product system");
			UI.label(comp, tk, config.system() != null ? config.system().name : null);

			UI.label(comp, tk, "Matching strategies");
			UI.label(comp, tk, strategiesText());
		}

		private void createCopiesSection(Composite parent, org.eclipse.ui.forms.widgets.FormToolkit tk) {
			var section = UI.section(parent, tk, "Providers copied to target database");
			UI.gridData(section, true, true);
			var comp = UI.sectionClient(section, tk, 1);
			var copiedTable = Tables.createViewer(comp, "Provider", "Flow", "Location");
			copiedTable.setLabelProvider(new ProviderInfoLabel());
			copiedTable.setInput(editor.plan.copies());
			Tables.bindColumnWidths2(copiedTable, 0.5, 0.3, 0.2);
			var table = copiedTable.getTable();
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			UI.gridData(table, true, true);
		}

		private void createMatchesSection(Composite parent, org.eclipse.ui.forms.widgets.FormToolkit tk) {
			var section = UI.section(parent, tk, "Provider matches");
			UI.gridData(section, true, true);
			var comp = UI.sectionClient(section, tk, 1);
			var matchesTable = Tables.createViewer(comp,
				"Provider",
				"Selected provider",
				"Status");
			matchesTable.setLabelProvider(new MatchLabel());
			matchesTable.setInput(editor.plan.matches());
			Tables.bindColumnWidths2(matchesTable, 0.42, 0.42, 0.16);
			var table = matchesTable.getTable();
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			UI.gridData(table, true, true);

			new ModifySupport<ProviderMatch>(matchesTable)
				.bind("Selected provider", new SelectedProviderModifier());
		}

		private String summaryText() {
			return editor.plan.matches().size() + " provider match"
				+ (editor.plan.matches().size() == 1 ? "" : "es")
				+ " found; " + editor.plan.copies().size() + " provider"
				+ (editor.plan.copies().size() == 1 ? "" : "s")
				+ " will be copied.";
		}

		private String strategiesText() {
			var strategies = editor.plan.config().strategies();
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
	}

	private static final class SelectedProviderModifier
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

	private static final class MatchLabel extends BaseLabelProvider
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
