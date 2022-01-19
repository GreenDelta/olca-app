package org.openlca.app.tools.openepd;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;

class ErrorDialog extends FormDialog {

	private final JsonObject message;

	static void show(JsonObject message) {
		if (message == null)
			return;
		new ErrorDialog(message).open();
	}

	private ErrorDialog(JsonObject message) {
		super(UI.shell());
		this.message = message;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Validation errors");
		newShell.setImage(Icon.ERROR.get());
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 600, 500);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		UI.formLabel(body, tk,
			"The upload failed with the following validation error:");
		var text = new GsonBuilder()
			.setPrettyPrinting()
			.create()
			.toJson(message);
		var widget = tk.createText(body, text,
			SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		UI.gridData(widget, true, true);
	}
}
