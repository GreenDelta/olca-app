package org.openlca.app.editors.reports;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.results.comparison.ProjectComparisonPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class ReportViewer extends SimpleFormEditor {

	public static String ID = "ReportViewer";
	private Logger log = LoggerFactory.getLogger(getClass());
	private Report report;
	public Project project;

	public static void open(Report report, Project project) {
		if (report == null)
			return;
		String reportID = Cache.getAppCache().put(report);
		var input = new SimpleEditorInput(reportID, report.title);
		Editors.open(input, ID);
	}

	public Report getReport() {
		return report;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		try {
			SimpleEditorInput sei = (SimpleEditorInput) input;
			this.report = Cache.getAppCache().remove(sei.id);
		} catch (Exception e) {
			String message = "failed to init report viewer";
			log.error(message, e);
			throw new PartInitException(message, e);
		}
	}
	
	@Override
	protected final void addPages() {
		try {
			addPage(getPage());
			addPage(new ProjectComparisonPage(this));
		} catch (Exception e) {
			log.error("Error adding page to " + getClass().getSimpleName(), e);
		}
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		public Page() {
			super(ReportViewer.this, "ReportPage", M.ReportView);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = mform.getForm();
			Composite comp = form.getBody();
			comp.setLayout(new FillLayout());
			try {
				Browser b = new Browser(comp, SWT.NONE);
				b.setJavascriptEnabled(true);
				UI.onLoaded(b, HtmlFolder.getUrl("report.html"), () -> {
					Gson gson = new Gson();
					String json = gson.toJson(report);
					String command = "setData(" + json + ")";
					b.execute(command);
				});
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to load report in browser", e);
			}
		}
	}
}
