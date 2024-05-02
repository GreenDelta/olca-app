package org.openlca.app.wizards.io;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.util.UI;
import org.openlca.core.io.ImportLog;
import org.openlca.io.Import;

public class ImportLogDialog extends FormDialog {

	private final String title;
	private final ImportLog log;

	public static void show(Import imp) {
		if (imp == null || imp.log() == null)
			return;
		var title = imp.isCanceled()
				? "Import canceled"
				: "Import finished";
		App.runInUI(title,
				() -> new ImportLogDialog(imp.log(), title).open());
	}

	private ImportLogDialog(ImportLog log, String title) {
		super(UI.shell());
		this.log = log;
		this.title = title;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 400, 300);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.DETAILS_ID,
				"Details...", false);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId != IDialogConstants.DETAILS_ID) {
			super.buttonPressed(buttonId);
			return;
		}
		ImportLogView.open(log);
		okPressed();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		UI.gridLayout(body, 1, 10, 25);

		long count = log.messages().stream()
				.filter(ImportLog.Message::hasDescriptor)
				.count();
		tk.createLabel(body, String.format("Handled %d data sets:", count))
				.setFont(UI.boldFont());

		var comp = tk.createComposite(body);
		UI.gridLayout(comp, 2, 5, 5);
		var states = new ImportLog.State[]{
				ImportLog.State.IMPORTED,
				ImportLog.State.UPDATED,
				ImportLog.State.SKIPPED,
				ImportLog.State.ERROR,
				ImportLog.State.WARNING
		};
		for (var state : states) {
			tk.createLabel(comp, headerOf(state));
			int c = log.countOf(state);
			tk.createLabel(comp, Integer.toString(c));
		}
	}

	private String headerOf(ImportLog.State state) {
		if (state == null)
			return "?";
		return switch (state) {
			case IMPORTED -> "Imported:";
			case UPDATED -> "Updated:";
			case SKIPPED -> "Skipped:";
			case ERROR -> "Errors:";
			case WARNING -> "Warnings:";
			case INFO -> "Other:";
		};
	}

}
