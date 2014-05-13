package org.openlca.app.editors.reports;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.html.HtmlPage;
import org.openlca.app.html.HtmlResource;
import org.openlca.app.html.IHtmlResource;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class ReportViewer extends FormEditor {

	public static String ID = "ReportViewer";
	private Logger log = LoggerFactory.getLogger(getClass());
	private Report report;

	public static void open(Report report) {
		Editors.open(new ReportEditorInput(report), ID);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		try {
			ReportEditorInput editorInput = (ReportEditorInput) input;
			this.report = editorInput.getReport();
		} catch (Exception e) {
			String message = "failed to init report viewer";
			log.error(message, e);
			throw new PartInitException(message, e);
		}
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
			super(ReportViewer.this, "olca.ReportPreview.Page",
					"Report Preview");
		}

		@Override
		public IHtmlResource getResource() {
			return new HtmlResource(RcpActivator.getDefault().getBundle(),
					"html/report_view.html", "report_view.html");
		}

		@Override
		public void onLoaded() {
			Gson gson = new Gson();
			String json = gson.toJson(report);
			String command = "setData(" + json + ")";
			try {
				browser.evaluate(command);
			} catch (Exception e) {
				log.error("failed to set report data to browser", e);
			}
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = managedForm.getForm();
			Composite composite = form.getBody();
			composite.setLayout(new FillLayout());
			browser = UI.createBrowser(composite, this);

			new BrowserFunction(browser, "calculate") {
				@Override
				public Object function(Object[] arguments) {
					Calculation.run(report.getProject());
					return super.function(arguments);
				}
			};

		}
	}
}
