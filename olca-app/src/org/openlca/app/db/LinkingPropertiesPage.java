package org.openlca.app.db;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;

public class LinkingPropertiesPage extends SimpleFormEditor {

	private LinkingProperties props;

	public static void show() {
		AtomicReference<LinkingProperties> ref = new AtomicReference<>();
		App.runWithProgress("#Check database links", () -> {
			LinkingProperties props = LinkingProperties.check(Database.get());
			ref.set(props);
		});
		LinkingProperties props = ref.get();
		if (props == null)
			return;
		String key = Cache.getAppCache().put(props);
		Editors.open(new Input(key), "editors.LinkingPropertiesPage");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		Input in = (Input) input;
		props = Cache.getAppCache().remove(
				in.key, LinkingProperties.class);
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {
		public Page() {
			super(LinkingPropertiesPage.this,
					"LinkingPropertiesPage.Page",
					"#Linking properties");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(mform, "#Linking properties");
			FormToolkit tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);
			form.reflow(true);
		}
	}

	private static class Input implements IEditorInput {

		private final String key;

		public Input(String key) {
			this.key = key;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.LINK.descriptor();
		}

		@Override
		public String getName() {
			return "#Linking properties";
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return getName();
		}
	}
}
