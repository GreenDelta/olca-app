package org.openlca.app.rcp.browser;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the 'about:config' page in an editor.
 */
public class MozillaConfigView extends FormEditor {

	public static String ID = "olca.MozillaConfigView";
	private Logger log = LoggerFactory.getLogger(getClass());

	public static boolean canShow() {
		return Config.useMozilla();
	}

	public static void open() {
		Editors.open(new EditorInput(), ID);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new Page());
		} catch (Exception e) {
			log.error("failed to add browser page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
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
