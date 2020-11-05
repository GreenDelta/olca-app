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

		var formText = tk.createFormText(comp, true);
		formText.setText(formMessage(), true, true);
		UI.gridData(formText, true, false).widthHint = 560;

		var text = tk.createText(comp, "", SWT.MULTI);
		UI.gridData(text, true, true);
		mform.reflow(true);
	}

	private String formMessage() {
		return "<html>"
				+"<p><b>"
				+ message
				+"</b></p>"
				+ "<p> If you think this should not happen or if there is "
				+ "something that we should improve please open an issue "
				+ "on the <a href='https://github.com/GreenDelta/olca-app'>"
				+ "openLCA Github repository</a> or send us an "
				+ "<a href='mailto:error@openlca.org'>email</a>. For "
				+ "reporting the issue, you can use the template below. "
				+ "Thanks!</p>"
				+ "</html>";
	}
}
