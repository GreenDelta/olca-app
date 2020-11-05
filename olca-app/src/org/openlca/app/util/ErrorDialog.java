package org.openlca.app.util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.rcp.images.Icon;

public class ErrorDialog extends FormDialog {

	private final String message;
	private final Throwable error;

	public static void open(String message, Throwable error) {
		App.runInUI(
				"Show error",
				() -> new ErrorDialog(message, error).open());
	}

	private ErrorDialog(String message, Throwable error) {
		super(UI.shell());
		this.message = message == null
				? "No error message"
				: message;
		this.error = error;
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setImage(Icon.ERROR.get());
		shell.setText("An error occurred");
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(
				parent,
				IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL,
				true);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 400);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		body.setLayout(new FillLayout());
		var comp = tk.createComposite(body);
		UI.gridLayout(comp, 1);
		var message = tk.createLabel(comp, this.message, SWT.WRAP);
		UI.gridData(message, true, false).widthHint = 560;

		var formText = tk.createFormText(comp, true);
		formText.setText(this.message, true, true);
		UI.gridData(formText, true, false).widthHint = 560;

		var text = tk.createText(comp, "", SWT.MULTI);
		UI.gridData(text, true, true);
		mform.reflow(true);
	}
}
