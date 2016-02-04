package org.openlca.app.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.navigation.actions.DatabaseImportAction;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.rcp.html.HtmlFolder;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.EclipseCommandLine;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartPage extends FormEditor {

	public static String ID = "olca.StartPage";
	private Logger log = LoggerFactory.getLogger(getClass());

	public static void open() {
		Editors.open(new StartPageInput(), ID);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new Page());
		} catch (Exception e) {
			log.error("failed to add start page", e);
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

	private class Page extends FormPage implements HtmlPage {

		private Browser browser;

		public Page() {
			super(StartPage.this, "olca.StartPage.Page", M.Welcome);
		}

		@Override
		public String getUrl() {
			String langCode = EclipseCommandLine.getArg("nl");
			if (langCode == null
					|| "en".equalsIgnoreCase(langCode)
					|| langCode.startsWith("en_"))
				return HtmlView.START_PAGE.getUrl();
			String pageName = "start_page_" + langCode + ".html";
			try {
				return HtmlFolder.getUrl(
						RcpActivator.getDefault().getBundle(), pageName);
			} catch (Exception e) {
				log.error("failed to get start page for language "
						+ langCode, e);
				return HtmlView.START_PAGE.getUrl();
			}
		}

		@Override
		public void onLoaded() {
			new ImportDatabaseCallback(browser);
			new OpenUrlCallback(browser);
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = managedForm.getForm();
			Composite composite = form.getBody();
			composite.setLayout(new FillLayout());
			browser = UI.createBrowser(composite, this);
		}

	}

	private class ImportDatabaseCallback extends BrowserFunction {
		public ImportDatabaseCallback(Browser browser) {
			super(browser, "importDatabase");
		}

		@Override
		public Object function(Object[] arguments) {
			log.trace("js-callback: importDatabase");
			new DatabaseImportAction().run();
			return null;
		}
	}

	private class OpenUrlCallback extends BrowserFunction {
		public OpenUrlCallback(Browser browser) {
			super(browser, "openUrl");
		}

		@Override
		public Object function(Object[] arguments) {
			log.trace("js-callback: openUrl");
			if (arguments == null || arguments.length == 0
					|| arguments[0] == null) {
				log.warn("openUrl: no url given");
				return null;
			}
			String url = arguments[0].toString();
			log.trace("open URL {}", url);
			Desktop.browse(url);
			return null;
		}
	}

	private static class StartPageInput implements IEditorInput {

		@Override
		@SuppressWarnings("rawtypes")
		public Object getAdapter(Class adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.LOGO.descriptor();
		}

		@Override
		public String getName() {
			return M.Welcome;
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return M.Welcome;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj == null)
				return false;
			if (obj instanceof StartPageInput)
				return true;
			else
				return false;
		}
	}
}
