package org.openlca.app.editors.projects.results;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
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
			TotalImpactSection.of(result)
				.renderOn(body, tk);
		}
	}


}
