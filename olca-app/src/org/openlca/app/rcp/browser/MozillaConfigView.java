package org.openlca.app.rcp.browser;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;

/**
 * Opens the 'about:config' page in an editor.
 */
public class MozillaConfigView extends SimpleFormEditor {

	public static String ID = "olca.MozillaConfigView";

	public static boolean canShow() {
		return Config.useMozilla();
	}

	public static void open() {
		Editors.open(new EditorInput(), ID);
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		public Page() {
			super(MozillaConfigView.this, "olca.MozillaConfigView.Page",
					"Browser configuration");
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = managedForm.getForm();
			Composite composite = form.getBody();
			composite.setLayout(new FillLayout());
			Browser browser = UI.createBrowser(composite);
			browser.setUrl("about:about");
		}
	}

	private static class EditorInput implements IEditorInput {

		@Override
		@SuppressWarnings("rawtypes")
		public Object getAdapter(Class adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		@Override
		public String getName() {
			return "Browser configuration";
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return "Browser configuration";
		}
	}

}
