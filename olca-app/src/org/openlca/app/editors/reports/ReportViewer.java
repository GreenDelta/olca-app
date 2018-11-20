package org.openlca.app.editors.reports;

import javafx.scene.web.WebEngine;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class ReportViewer extends SimpleFormEditor {

	public static String ID = "ReportViewer";
	private Logger log = LoggerFactory.getLogger(getClass());
	private Report report;

	public static void open(Report report) {
		if (report == null)
			return;
		Editors.open(new EditorInput(report), ID);
	}

	public Report getReport() {
		return report;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		try {
			EditorInput editorInput = (EditorInput) input;
			this.report = editorInput.getReport();
		} catch (Exception e) {
			String message = "failed to init report viewer";
			log.error(message, e);
			throw new PartInitException(message, e);
		}
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private static class EditorInput implements IEditorInput {
		private final Report report;

		public EditorInput(Report report) {
			this.report = report;
		}

		public Report getReport() {
			return report;
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Object getAdapter(Class adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return report != null;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Images.descriptor(ModelType.PROJECT);
		}

		@Override
		public String getName() {
			String name = report.title != null ? report.title
					: M.Report;
			return Strings.cut(name, 75);
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return report.title != null ? report.title
					: M.Report;
		}
	}

	private class Page extends FormPage implements WebPage {

		public Page() {
			super(ReportViewer.this, "olca.ReportPreview.Page",
					M.ReportView);
		}

		@Override
		public String getUrl() {
			return HtmlView.REPORT_VIEW.getUrl();
		}

		@Override
		public void onLoaded(WebEngine webkit) {
			Gson gson = new Gson();
			String json = gson.toJson(report);
			String messages = M.asJson();
			String command = "setData(" + json + ", " + messages + ")";
			try {
				webkit.executeScript(command);
			} catch (Exception e) {
				log.error("failed to set report data to browser", e);
			}
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = managedForm.getForm();
			Composite composite = form.getBody();
			composite.setLayout(new FillLayout());
			UI.createWebView(composite, this);
		}
	}
}
