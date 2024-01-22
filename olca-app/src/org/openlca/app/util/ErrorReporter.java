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
import org.openlca.nativelib.Module;
import org.openlca.nativelib.NativeLib;
import org.slf4j.LoggerFactory;

public class ErrorReporter extends FormDialog {

	private final String message;
	private final String details;
	private final Throwable error;

	public static void on(String message) {
		on(message, null, null);
	}

	public static void on(String message, String details) {
		on(message, details, null);
	}

	/**
	 * Opens the error reporter for the given message and error. It also writes the
	 * error to the log. So no need to log an error when you later want to open it
	 * in the reporter. It is safe to call this method from non-UI threads.
	 */
	public static void on(String message, Throwable error) {
		on(message, null, error);
	}

	public static void on(String message, String details, Throwable error) {
		var log = LoggerFactory.getLogger(ErrorReporter.class);
		log.error(message, error);
		App.runInUI("Show error reporter",
				() -> new ErrorReporter(message, details, error).open());
	}

	private ErrorReporter(String message, String details, Throwable error) {
		super(UI.shell());
		this.message = message == null
				? "No error message"
				: message;
		this.details = details;
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
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		body.setLayout(new FillLayout());
		var comp = tk.createComposite(body);
		UI.gridLayout(comp, 1);

		var formText = tk.createFormText(comp, true);
		formText.setText(formMessage(), true, true);
		UI.gridData(formText, true, false).widthHint = 560;

		var text = tk.createText(comp, "", SWT.MULTI | SWT.V_SCROLL);
		UI.gridData(text, true, true);
		form.reflow(true);
		var message = details != null
				? "# Error details: \n" + details + "\n\n" + reportTemplate()
				: reportTemplate();
		text.setText(message);

		formText.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				var href = e.getHref();
				if (href == null)
					return;
				var uri = href.toString();
				if (uri.startsWith("mailto:")) {
					uri += "?subject=openLCA%20error&body=";
					uri += URLEncoder.encode(
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

	private String reportTemplate() {
		return "If you want to report this error to us, please replace the\n" +
				"question marks in the template below (if possible).\n" +
				"Thanks a lot!\n\n" +
				"# Error description\n" +
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
				"# openLCA error message\n" +
				"\n" +
				message +
				"\n\n" +
				"# Installation details\n" +
				"\n" +
				"* openLCA version: " + App.getVersion() + "\n" +
				"* operating system: " + System.getProperty("os.name") + "\n" +
				"* os architecture: " + System.getProperty("os.arch") + "\n" +
				"* os version: " + System.getProperty("os.version") + "\n" +
				"* native libraries loaded: " + NativeLib.isLoaded() + "\n" +
				"* with sparse matrix support: " +  NativeLib.isLoaded(Module.UMFPACK)  + "\n" +
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
