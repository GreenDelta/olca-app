package org.openlca.app.editors.results.openepd;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.openlca.core.model.ResultModel;
import org.openlca.util.Strings;

public class ExportDialog extends FormDialog {

	private final ResultModel result;
	private final Credentials credentials;

	private ExportDialog(ResultModel result) {
		super(UI.shell());
		this.result = result;
		setBlockOnOpen(true);
		setShellStyle(SWT.CLOSE
			| SWT.MODELESS
			| SWT.BORDER
			| SWT.TITLE
			| SWT.RESIZE
			| SWT.MIN);
		credentials = Credentials.init();
	}

	public static int show(ResultModel result) {
		return new ExportDialog(result).open();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Export as OpenEPD to EC3");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 300);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		credentialsSection(body, tk);
	}

	private void credentialsSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "EC3 Login");
		var comp = UI.sectionClient(section, tk, 2);

		// url
		var filled = 0;
		var urlText = UI.formText(comp, tk, "URL");
		if (Strings.notEmpty(credentials.url)) {
			urlText.setText(credentials.url);
			filled++;
		}
		urlText.addModifyListener($ -> {
			credentials.url = urlText.getText();
		});

		// user
		var userText = UI.formText(comp, tk, "User");
		if (Strings.notEmpty(credentials.user)) {
			userText.setText(credentials.user);
			filled++;
		}
		userText.addModifyListener($ -> {
			credentials.user = userText.getText();
		});

		// password
		var pwText = UI.formText(comp, tk, "Password", SWT.PASSWORD);
		if (Strings.notEmpty(credentials.password)) {
			pwText.setText(credentials.password);
			filled++;
		}
		pwText.addModifyListener($ -> {
			credentials.password = pwText.getText();
		});

		section.setExpanded(filled < 3);

	}


}
