package org.openlca.app.editors.projects;

import java.util.Optional;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.Project;

public class ProjectEditor extends ModelEditor<Project> {

	public static String ID = "editors.project";
	Report report;

	public ProjectEditor() {
		super(Project.class);
	}

	public Report report() {
		return report;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProjectSetupPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("Failed to add project pages", e);
		}
	}

}
