package org.openlca.app.editors.projects.reports;

import java.io.IOException;

import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Project;
import org.openlca.core.results.ProjectResult;
import org.openlca.util.Pair;

public class ReportEditor extends SimpleFormEditor {

	Project project;
	ProjectResult result;
	Report report;

	public static void open(Project project, ProjectResult result) {
		if (project == null || result == null)
			return;
		var pair = Pair.of(project, result);
		var id = Cache.getAppCache().put(pair);
		var input = new SimpleEditorInput(
			id, "Report of: " + Labels.name(project));
		Editors.open(input, "ReportEditor");
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
		this.report = new Report();
		setPartName("Report of: " + Labels.name(project));
	}

	@Override
	protected FormPage getPage() {
		return new ReportEditorPage(this);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void doSaveAs() {
		var file = FileChooser.forSavingFile(
			"Save report template as file", "openLCA report template.json");
		if (file == null)
			return;
		try {
			var json = new GsonBuilder()
				.setPrettyPrinting()
				.create()
				.toJson(report);
			FileUtils.write(file, json, "utf-8");
		} catch (IOException e) {
			ErrorReporter.on("Failed to save report template", e);
		}
	}
}
