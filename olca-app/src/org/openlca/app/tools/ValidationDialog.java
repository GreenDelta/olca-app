package org.openlca.app.tools;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.validation.Validation;

public class ValidationDialog extends FormDialog {

	private final IDatabase db;
	private int maxItems = 1000;

	private Validation validation;
	private Combo combo;
	private Spinner spinner;
	private Label infoLabel;
	private ProgressBar progressBar;

	public static void show() {
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened);
			return;
		}
		var dialog = new ValidationDialog(db);
		dialog.open();
	}

	private ValidationDialog(IDatabase db) {
		super(UI.shell());
		this.db = db;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Validate database " + db.getName());
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 450, 250);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 2);

		// types of messages that should be collected
		combo = UI.formCombo(body, tk, "Validation messages");
		combo.setItems(
			"All messages",
			"Warnings and errors",
			"Errors only");
		combo.select(1);
		UI.gridData(combo, true, false);

		// max. items
		UI.formLabel(body, tk, "Maximum message count");
		spinner = new Spinner(body, SWT.BORDER);
		tk.adapt(spinner);
		spinner.setValues(maxItems, 0, Integer.MAX_VALUE, 0, 100, 1000);
		Controls.onSelect(
			spinner, e -> maxItems = spinner.getSelection());

		// progress bar and message
		var progressComp = tk.createComposite(body);
		UI.gridData(progressComp, true, false).horizontalSpan = 2;
		UI.gridLayout(progressComp, 1);
		infoLabel = tk.createLabel(
			progressComp, "Validation is running ...");
		UI.gridData(infoLabel, true, false);
		infoLabel.setVisible(false);
		progressBar = new ProgressBar(progressComp, SWT.SMOOTH);
		UI.gridData(progressBar, true, false);
		progressBar.setVisible(false);
	}

	@Override
	protected void okPressed() {
		var okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(false);
		}

		// set UI in `run`-mode
		combo.setEnabled(false);
		spinner.setEnabled(false);
		infoLabel.setVisible(true);
		progressBar.setVisible(true);
		progressBar.setSelection(0);
		var display = progressBar.getDisplay();

		// start the validation thread
		int level = combo.getSelectionIndex();
		validation = Validation.on(db)
			.maxItems(maxItems)
			.skipInfos(level > 0)
			.skipWarnings(level > 1);
		new Thread(validation).start();

		// UI thread for updating the dialog
		new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(100);
					if (validation.hasFinished()) {
						ValidationResultView.open(validation.items());
						display.asyncExec(super::okPressed);
						break;
					}
					display.asyncExec(() -> {
						progressBar.setMaximum(validation.workerCount() + 1);
						progressBar.setSelection(1 + validation.finishedWorkerCount());
					});
				} catch (InterruptedException e) {
					ErrorReporter.on("failed to wait during validation", e);
					close();
				}
			}
		}).start();
	}

	@Override
	protected void cancelPressed() {
		if (validation == null) {
			super.cancelPressed();
			return;
		}
		var cancelButton = getButton(IDialogConstants.CANCEL_ID);
		if (cancelButton != null) {
			cancelButton.setEnabled(false);
		}
		infoLabel.setText("Cancelling validation ...");
		infoLabel.getParent().redraw();
		validation.cancel();
	}
}
