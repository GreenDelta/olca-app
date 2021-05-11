package org.openlca.app.editors.projects.results;

import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.editors.projects.reports.ReportEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Project;
import org.openlca.core.results.ProjectResult;
import org.openlca.util.Pair;

public class ProjectResultEditor extends SimpleFormEditor {

	private Project project;
	private ProjectResult result;

	public static void open(Project project, ProjectResult result) {
		if (project == null || result == null)
			return;
		var pair = Pair.of(project, result);
		var id = Cache.getAppCache().put(pair);
		var input = new SimpleEditorInput(
			id, "Result of: " + Labels.name(project));
		Editors.open(input, "ProjectResultEditor");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		var simpleInput = (SimpleEditorInput) input;
		var obj = Cache.getAppCache().remove(simpleInput.id);
		var err = "editor input must be a pair of project and result";
		if (!(obj instanceof Pair))
			throw new PartInitException(err);
		var pair = (Pair<?, ?>) obj;
		var first = pair.first;
		var second = pair.second;
		if (!(first instanceof Project)
				|| !(second instanceof ProjectResult))
			throw new PartInitException(err);
		this.project = (Project) first;
		this.result = (ProjectResult) second;
		setPartName("Result of: " + Labels.name(project));
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private final Project project;
		private final ProjectResult result;

		Page(ProjectResultEditor editor) {
			super(editor, "ProjectResultEditor.Page", "Results");
			this.project = editor.project;
			this.result = editor.result;
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform,
				"Results of: " + Labels.name(project));
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);

			var buttonComp = tk.createComposite(body);
			UI.gridLayout(buttonComp, 2);
			var excelBtn = tk.createButton(
				buttonComp, M.ExcelExport, SWT.NONE);
			excelBtn.setImage(Images.get(FileType.EXCEL));
			UI.gridData(excelBtn, false, false).widthHint = 120;

			// report button
			var reportBtn = tk.createButton(
				buttonComp, "Create Report", SWT.NONE);
			reportBtn.setImage(Images.get(ModelType.PROJECT));
			UI.gridData(reportBtn, false, false).widthHint = 120;
			Controls.onSelect(
				reportBtn, $ -> ReportEditor.open(project, result));

			ProjectVariantSection.of(result).renderOn(body, tk);
			TotalImpactSection.of(result).renderOn(body, tk);

			if (project.nwSet != null) {
				var nwFactors = NwSetTable.of(Database.get(), project.nwSet);
				if (nwFactors.hasNormalization()) {
					NwSection.forNormalization(result, nwFactors).renderOn(body, tk);
				}
				if (nwFactors.hasWeighting()) {
					NwSection.forWeighting(result, nwFactors)
						.withUnit(project.nwSet.weightedScoreUnit)
						.renderOn(body, tk);
					SingleScoreSection.of(result, nwFactors)
						.withUnit(project.nwSet.weightedScoreUnit)
						.renderOn(body, tk);
				}
			}
		}
	}
}
