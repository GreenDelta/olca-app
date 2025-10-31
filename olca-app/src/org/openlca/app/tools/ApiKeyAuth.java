package org.openlca.app.tools;

import java.io.File;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.jsonld.Json;
import org.openlca.util.Res;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/// This is a simple authentication utility for APIs that only need an endpoint
/// URL and an API key. The API key can be cached in a workspace file if the
/// user wants this.
public class ApiKeyAuth<T> extends FormDialog {

	private final String defaultUrl;
	private final Function<ApiKey, Res<T>> login;
	private ApiKey key;
	private T client;

	private Text urlText;
	private Text apiKeyText;

	private boolean cacheIt = true;

	private ApiKeyAuth(String defaultUrl, Function<ApiKey, Res<T>> login) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.defaultUrl = defaultUrl;
		this.login = login;
	}

	public static <T> Optional<T> fromCacheOrDialog(
			String fileName, String endpoint, Function<ApiKey, Res<T>> login
	) {

		// first try to use a cached API key
		var key = ApiKey.readFromWorkspace(fileName).orElse(null);
		if (key != null) {
			var client = App.exec(
					"Connecting to API with cached API key...",
					() -> login.apply(key));
			if (!client.hasError())
				return Optional.of(client.value());
			LoggerFactory.getLogger(ApiKeyAuth.class)
					.info("invalid API key in file {}: {}", fileName, client.error());
		}

		// ask for the API key in a dialog
		var dialog = new ApiKeyAuth<>(endpoint, login);
		if (dialog.open() != OK || dialog.client == null)
			return Optional.empty();
		if (dialog.cacheIt && dialog.key != null) {
			dialog.key.writeToWorkspace(fileName);
		}
		return Optional.of(dialog.client);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Please enter your API key");
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

		urlText = UI.labeledText(comp, tk, "URL");
		urlText.setText(defaultUrl);
		urlText.addModifyListener(e -> checkOk());
		apiKeyText = UI.labeledText(comp, tk, "API key");
		apiKeyText.addModifyListener(e -> checkOk());

		UI.filler(comp);
		var saveCheck = UI.checkbox(comp, tk, "Save API key");
		saveCheck.setSelection(cacheIt);
		Controls.onSelect(saveCheck, $ -> cacheIt = saveCheck.getSelection());
	}

	private void checkOk() {
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

		var key = new ApiKey(
				urlText.getText().strip(),
				apiKeyText.getText().strip());

		var res = App.exec("Connecting to API ...", () -> login.apply(key));
		if (res.hasError()) {
			MsgBox.error("Connection to API failed", res.error());
			return;
		}

		this.key = key;
		this.client = res.value();
		super.okPressed();
	}

	public record ApiKey(String endpoint, String value) {

		boolean isEmpty() {
			return Strings.isBlank(endpoint)
					|| Strings.isBlank(value);
		}

		static Optional<ApiKey> readFromWorkspace(String fileName) {
			var file = new File(Workspace.root(), fileName);
			if (!file.exists())
				return Optional.empty();
			var obj = Json.readObject(file).orElse(null);
			if (obj == null)
				return Optional.empty();
			var key = new ApiKey(
					Json.getString(obj, "url"),
					Json.getString(obj, "apiKey"));
			return key.isEmpty()
					? Optional.empty()
					: Optional.of(key);
		}

		void writeToWorkspace(String fileName) {
			var file = new File(Workspace.root(), fileName);
			var obj = new JsonObject();
			obj.addProperty("url", endpoint);
			obj.addProperty("apiKey", value);
			Json.write(obj, file);
		}
	}
}
