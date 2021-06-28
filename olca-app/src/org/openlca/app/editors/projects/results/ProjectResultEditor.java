package org.openlca.app.editors.projects.results;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.projects.ProjectResultData;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.io.xls.results.ProjectResultExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectResultEditor extends FormEditor {

	ProjectResultData data;

	public static void open(ProjectResultData data) {
		if (data == null)
			return;
		var id = Cache.getAppCache().put(data);
		var input = new SimpleEditorInput(
			id, "Result of: " + Labels.name(data.project()));
		Editors.open(input, "ProjectResultEditor");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		var simpleInput = (SimpleEditorInput) input;
		var obj = Cache.getAppCache().remove(simpleInput.id);
		if (!(obj instanceof ProjectResultData))
			throw new PartInitException("editor input must be a project result");
		data = (ProjectResultData) obj;
		setPartName("Result of: " + Labels.name(data.project()));
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ResultPage(this));
			if (data.hasReport()) {
				addPage(new ReportPage());
			}
		} catch (Exception e) {
			ErrorReporter.on("Failed to open project result", e);
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

	private static class ResultPage extends FormPage {

		private final ProjectResultData data;

		ResultPage(ProjectResultEditor editor) {
			super(editor, "ProjectResultEditor.Page", "Results");
			this.data = editor.data;
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform,
				"Results of: " + Labels.name(data.project()));
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);

			var buttonComp = tk.createComposite(body);
			UI.gridLayout(buttonComp, 1);

			// Excel export button
			var excelBtn = tk.createButton(
				buttonComp, M.ExcelExport, SWT.NONE);
			excelBtn.setImage(Images.get(FileType.EXCEL));
			UI.gridData(excelBtn, false, false).widthHint = 120;
			Controls.onSelect(excelBtn, $ -> {
				var file = FileChooser.forSavingFile(
					"Export project result", "project result.xlsx");
				if (file == null)
					return;
				var export = new ProjectResultExport(
					data.project(), data.result(), data.db());
				try {
					export.writeTo(file);
					Popup.info("Exported results to " + file.getName());
				} catch (Exception e) {
					ErrorReporter.on("Export of project result failed", e);
				}
			});

			// create the sections
			ProjectVariantSection.of(data).renderOn(body, tk);
			TotalImpactSection.of(data).renderOn(body, tk);
			if (data.hasNormalization()) {
				NwSection.forNormalization(data).renderOn(body, tk);
			}
			if (data.hasWeighting()) {
				NwSection.forWeighting(data).renderOn(body, tk);
				SingleScoreSection.of(data).renderOn(body, tk);
			}
			ContributionSection.of(data).renderOn(body, tk);
		}
	}

	private class ReportPage extends FormPage {

		ReportPage() {
			super(ProjectResultEditor.this, "ReportPage", M.Report);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = mform.getForm();
			var comp = form.getBody();
			comp.setLayout(new FillLayout());
			try {
				var browser = new Browser(comp, SWT.NONE);
				browser.setJavascriptEnabled(true);
				UI.onLoaded(browser, HtmlFolder.getUrl("report.html"), () -> {
					var json = data.report().fillWith(data).toJson();
					browser.execute("setData(" + json + ")");
				});
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to load report in browser", e);
			}
		}
	}
}
