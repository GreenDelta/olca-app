package org.openlca.app.tools.openepd.output;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;

class JsonErrorDialog extends FormDialog {

	private final String title;
	private final JsonElement json;

	static void show(String title, JsonElement json) {
		if (json == null)
			return;
		new JsonErrorDialog(title, json).open();
	}

	private JsonErrorDialog(String title, JsonElement json) {
		super(UI.shell());
		this.title = title;
		this.json = json;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Error");
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
		UI.formLabel(body, tk, title);
		var text = new GsonBuilder()
			.setPrettyPrinting()
			.create()
			.toJson(json);
		var widget = tk.createText(body, text,
			SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		UI.gridData(widget, true, true);
	}
}
