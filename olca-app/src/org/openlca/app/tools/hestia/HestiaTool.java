package org.openlca.app.tools.hestia;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.ApiKeyAuth;
import org.openlca.app.util.UI;
import org.openlca.io.hestia.HestiaClient;
import org.openlca.util.Res;

public class HestiaTool extends SimpleFormEditor {

	private HestiaClient client;

	public static void open() {

		var client = ApiKeyAuth.fromCacheOrDialog(
				".hestia.json", "https://api.hestia.earth", key -> {
					var c = HestiaClient.of(key.endpoint(), key.value());
					// TODO: check /users/me
					return Res.of(c);
				}
		);

		if (client.isEmpty())
			return;
		var id = AppContext.put(client.get());
		var input = new SimpleEditorInput(id, "Hestia");
		Editors.open(input, "HestiaTool");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		var inp = (SimpleEditorInput) input;
		client = AppContext.remove(inp.id, HestiaClient.class);
		if (client == null)
			throw new PartInitException("failed to get the Hestia client");
		setTitleImage(Icon.CONNECT.get());
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private final HestiaClient client;

		Page(HestiaTool editor) {
			super(editor, "Hestia", "Hestia");
			this.client = editor.client;
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.header(mForm, "Hestia");
			var tk = mForm.getToolkit();
			var body = UI.body(form, tk);

			// TODO: Add TreeMenu.mountOn(client, tree) when TreeMenu is implemented

			App.runInUI("Fetching data ...", () -> {
				// TODO: Implement TreeModel.fetch(client) when TreeModel is available

			});
		}


	}
}
