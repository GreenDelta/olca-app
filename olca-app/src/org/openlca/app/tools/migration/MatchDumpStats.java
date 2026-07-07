package org.openlca.app.tools.migration;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;

record MatchDumpStats(
	int planCount,
	int dumpCount,
	int foundCount,
	int planUpdates
) {

	void showDialog() {
		new StatsDialog(this).open();
	}

	private static class StatsDialog extends FormDialog {

		private final MatchDumpStats stats;

		StatsDialog(MatchDumpStats stats) {
			super(UI.shell());
			this.stats = stats;
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setText("Updated the migration plan");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.dialogBody(mform.getForm(), tk);
			UI.gridLayout(body, 2, 10, 25);

			tk.createLabel(body, "Matched providers in migration plan:");
			num(body, tk, stats.planCount);

			tk.createLabel(body, "Matched providers in backup:");
			num(body, tk, stats.dumpCount);

			tk.createLabel(body, "Identical providers in migration plan and backup:");
			num(body, tk, stats.foundCount);

			tk.createLabel(body, "Updated provider assignments:");
			num(body, tk, stats.planUpdates).setFont(UI.boldFont());
		}

		private Label num(Composite body, FormToolkit tk, int num) {
			return tk.createLabel(body, Integer.toString(num));
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		}

		@Override
		protected Point getInitialSize() {
			return UI.initialSizeOf(this, 400, 200);
		}
	}
}
