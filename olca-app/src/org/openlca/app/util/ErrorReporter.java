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
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.commons.Strings;
import org.openlca.core.matrix.solvers.mkl.MKL;
import org.openlca.nativelib.Module;
import org.openlca.nativelib.NativeLib;
import org.slf4j.LoggerFactory;

/// Logs an unexpected error and shows a dialog with diagnostic details.
/// The static `on(...)` methods are safe to call from non-UI threads; the
/// dialog is opened on the UI thread.
public class ErrorReporter extends FormDialog {

	private final String title;
	private final String details;
	private final Throwable error;

	public static void on(String title) {
		on(title, null, null);
	}

	public static void on(String title, String details) {
		on(title, details, null);
	}

	public static void on(String title, Throwable error) {
		on(title, null, error);
	}

	public static void on(String title, String details, Throwable error) {
		var log = LoggerFactory.getLogger(ErrorReporter.class);
		log.error(title, error);
		App.runInUI(M.ShowErrorReporter,
			() -> new ErrorReporter(title, details, error).open());
	}

	private ErrorReporter(String title, String details, Throwable error) {
		super(UI.shell());
		this.title = title == null
			? "No error message"
			: title;
		this.details = details;
		this.error = error;
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setImage(Icon.ERROR.get());
		var shellTitle = Strings.isNotBlank(title)
			? title
			: M.AnErrorOccurred;
		shell.setText(shellTitle);
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
		var textGrid = UI.gridData(text, true, true);
		textGrid.widthHint = 1;
		textGrid.heightHint = 1;
		text.setText(errorText());

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
		var formTitle = title;
		if (error != null) {
			var cause = error;
			while (cause.getCause() != null) {
				cause = cause.getCause();
			}
			var causeMsg = cause.getMessage();
			if (Strings.isNotBlank(causeMsg)) {
				formTitle = Strings.cutEnd(causeMsg, 125);
			}
		}

		return "<html><p><b>" + formTitle + "</b></p>"
			+ "<p> This was an unexpected error. If you want to report it, please "
			+ "copy the details below and report the problem in our "
			+ "<a href='https://github.com/GreenDelta/olca-app'>GitHub repository</a> "
			+ "or by <a href='mailto:error@openlca.org'>email</a>. If possible, "
			+ "include the steps needed to reproduce the problem.</p>"
			+ "</html>";
	}

	private String errorText() {
		var header = "# " + title + "\n\n";
		if (Strings.isNotBlank(details)) {
			header += "## Error details\n\n" + details + "\n\n";
		}
		return header +
			"## Installation details\n\n" +
			"- openLCA version: " + App.getVersion() + "\n" +
			"- Operating system: " + System.getProperty("os.name") + "\n" +
			"- OS architecture: " + System.getProperty("os.arch") + "\n" +
			"- OS version: " + System.getProperty("os.version") + "\n" +
			"- Native library: " + nativeLib() + "\n" +
			"\n" +
			"## Full error stack trace\n" +
			"\n" +
			"```\n" +
			stackTrace() +
			"\n```";
	}

	private String nativeLib() {
		if (MKL.isLoaded())
			return "Intel MKL";
		if (NativeLib.isLoaded()) {
			return NativeLib.isLoaded(Module.UMFPACK)
				? "OpenBLAS & UMFPACK"
				: "OpenBLAS";
		}
		return "None";
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
