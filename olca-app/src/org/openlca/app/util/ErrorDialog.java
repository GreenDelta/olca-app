package org.openlca.app.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.openlca.app.App;
import org.openlca.app.rcp.images.Icon;
import org.openlca.julia.Julia;

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

		var text = tk.createText(comp, "", SWT.MULTI | SWT.V_SCROLL);
		UI.gridData(text, true, true);
		mform.reflow(true);
		text.setText(template());
		
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				var href = e.getHref();
				if (href == null)
					return;
				var uri = href.toString();
				if (uri.startsWith("mailto:")) {
					uri += "?subject=openLCA%20error&body=";
					uri +=URLEncoder.encode(
							text.getText(), StandardCharsets.US_ASCII)
							.replace("+", "%20");
				}
				Desktop.browse(uri);
			}
		});
	}

	private String formMessage() {
		return "<html>"
				+ "<p><b>"
				+ message
				+ "</b></p>"
				+ "<p> If you think this should not happen or if there is "
				+ "something that we should improve please open an issue "
				+ "on our <a href='https://github.com/GreenDelta/olca-app'>"
				+ "openLCA Github repository</a> or send us an "
				+ "<a href='mailto:error@openlca.org'>email</a>. For "
				+ "reporting the issue, you can use the template below. "
				+ "Thanks!</p>"
				+ "</html>";
	}

	private String template() {
		return "# Error description\n" +
				"\n" +
				"I tried to ??? but openLCA threw an error.\n" +
				"\n" +
				"# Steps to reproduce\n" +
				"\n" +
				"1. ?\n" +
				"2. ?\n" +
				"3. ?\n" +
				"\n" +
				"# Attached files\n" +
				"\n" +
				"* example database ?\n" +
				"* screen shots ?\n" +
				"* ...\n" +
				"\n" +
				"# Installation details\n" +
				"\n" +
				"* openLCA version: " + App.getVersion() + "\n" +
				"* operating system: " + System.getProperty("os.name") + "\n" +
				"* os architecture: " + System.getProperty("os.arch") + "\n" +
				"* os version: " + System.getProperty("os.version") + "\n" +
				"* native libraries loaded: " + Julia.isLoaded() + "\n" +
				"* with sparse matrix support: " + Julia.hasSparseLibraries() + "\n" +
				"\n" +
				"# Full error stack trace\n" +
				"\n" +
				"```\n" +
				stackTrace() +
				"\n```";
	}

	private String stackTrace() {
		if (error == null)
			return "not available";
		var writer = new StringWriter();
		var printer = new PrintWriter(writer);
		error.printStackTrace(printer);
		return writer.toString();
	}
}
