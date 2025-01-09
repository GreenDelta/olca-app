package org.openlca.app.tools.smartepd;

import java.util.Optional;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.io.smartepd.SmartEpdClient;
import org.slf4j.LoggerFactory;

class AuthDialog extends FormDialog {

	private Text urlText;
	private Text apiKeyText;
	private Con con;

	static Optional<Con> show() {
		var dialog = new AuthDialog();
		return dialog.open() == OK
				? Optional.ofNullable(dialog.con)
				: Optional.empty();
	}

	private AuthDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Connect to the SmartEPD API");
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 600, 400);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		var comp = tk.createComposite(body);
		UI.fillHorizontal(comp);
		UI.gridLayout(comp, 2);

		var label = UI.label(comp, tk,
				"Please provide the URL and your API key for the SmartEPD API");
		UI.gridData(label, false, false).horizontalSpan = 2;

		urlText = UI.labeledText(comp, tk, "URL");
		urlText.setText("https://smart-epd.herokuapp.com/api");
		urlText.addModifyListener(e -> checkOk());
		apiKeyText = UI.labeledText(comp, tk, "API key");
		apiKeyText.addModifyListener(e -> checkOk());
	}

	private void checkOk() {
		con = null;
		var url = urlText.getText().strip();
		var apiKey = apiKeyText.getText().strip();
		var btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) {
			btn.setEnabled(!url.isBlank() && !apiKey.isBlank());
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		var ok = getButton(IDialogConstants.OK_ID);
		if (ok != null) {
			ok.setEnabled(false);
		}
	}

	@Override
	protected void okPressed() {
		var auth = Auth.of(urlText.getText(), apiKeyText.getText());
		var res = App.exec("Test API connection", auth::createClient);
		if (res.hasError()) {
			MsgBox.error("Failed to create API connection",
					"Please check the URL and API key.");
			LoggerFactory.getLogger(getClass())
					.error("failed to connect to SmartEPD API: {}", res.error());
			return;
		}
		con = new Con(auth, res.value());
		super.okPressed();
	}

	record Con(Auth auth, SmartEpdClient client) {
	}
}
