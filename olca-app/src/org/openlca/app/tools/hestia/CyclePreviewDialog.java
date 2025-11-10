package org.openlca.app.tools.hestia;

import java.time.ZoneId;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.commons.Strings;
import org.openlca.io.hestia.Cycle;
import org.openlca.io.hestia.HestiaClient;
import org.openlca.io.hestia.HestiaExchange;
import org.openlca.jsonld.Json;

class CyclePreviewDialog extends FormDialog {

	private final Cycle cycle;

	static void show(HestiaClient client, String id) {
		if (client == null || id == null)
			return;
		var res = App.exec("Fetch cycle from API ...", () -> client.getCycle(id));
		if (res.isError()) {
			MsgBox.error("Failed to fetch cycle",
					"Failed to fetch cycle " + id + ": " + res.error());
			return;
		}
		var dialog = new CyclePreviewDialog(res.value());
		dialog.open();
	}

	private CyclePreviewDialog(Cycle cycle) {
		super(UI.shell());
		this.cycle = cycle;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Cycle: " + cycle.name());
	}

	@Override
	protected Point getInitialSize() {
		return new Point(900, 700);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		createInfoSection(body, tk);
		createTable(body, tk, "Inputs", cycle.inputs());
		createTable(body, tk, "Products", cycle.products());
		createTable(body, tk, "Emissions", cycle.emissions());
		createTable(body, tk, "Practices", cycle.practices());
		mForm.reflow(true);
	}

	private void createInfoSection(Composite parent, FormToolkit tk) {
		var comp = UI.formSection(parent, tk, M.GeneralInformation, 2);

		var nameText = UI.labeledText(comp, tk, M.Name);
		Controls.set(nameText, cycle.name());
		nameText.setEditable(false);

		var infoText = UI.multiText(comp, tk, M.Description);
		Controls.set(infoText, cycle.description());
		infoText.setEditable(false);

		var startText = UI.labeledText(comp, tk, M.StartDate);
		var start = cycle.startDate();
		if (start != null) {
			var s = start.toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDate()
					.toString();
			startText.setText(s);
		}
		startText.setEditable(false);

		var endText = UI.labeledText(comp, tk, M.EndDate);
		var end = cycle.endDate();
		if (end != null) {
			var s = end.toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDate()
					.toString();
			endText.setText(s);
		}
		endText.setEditable(false);
	}

	private void createTable(
			Composite parent,
			FormToolkit tk,
			String title,
			List<? extends HestiaExchange> exchanges
	) {

		if (!exchanges.isEmpty()) {
			exchanges.sort((e1, e2) -> {
				var t1 = e1.term();
				var t2 = e2.term();
				if (t1 == null && t2 == null)
					return 0;
				if (t1 == null || t2 == null)
					return t1 == null ? -1 : 1;
				return Strings.compareIgnoreCase(t1.name(), t2.name());
			});
		}

		var comp = UI.formSection(parent, tk, title, 1);
		var table = Tables.createViewer(comp,
				"ID", "Name", "Type", "Amount", "Unit");
		table.setLabelProvider(new TableLabel());
		Tables.bindColumnWidths(table, 0.15, 0.35, 0.2, 0.15, 0.15);
		table.setInput(exchanges);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, 9999, "Export", false);
		createButton(parent, IDialogConstants.OK_ID, "Close", true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId != 9999) {
			super.buttonPressed(buttonId);
			return;
		}
		var file = FileChooser.forSavingFile(
				"Write cylce as JSON file",
				cycle.name() + ".json");
		if (file != null) {
			Json.write(cycle.json(), file);
		}
	}

	private static class TableLabel extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof HestiaExchange e))
				return null;
			var term = e.term();
			if (term == null)
				return null;
			return switch (col) {
				case 0 -> term.id();
				case 1 -> term.name();
				case 2 -> term.termType();
				case 3 -> Double.toString(e.value());
				case 4 -> term.unit();
				default -> null;
			};
		}
	}
}
