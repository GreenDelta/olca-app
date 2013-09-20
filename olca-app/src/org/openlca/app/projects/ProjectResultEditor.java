package org.openlca.app.projects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.core.database.ProjectDao;
import org.openlca.core.model.Project;
import org.openlca.core.results.ProjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectResultEditor extends FormEditor {

	public static final String ID = "ProjectResultEditor";

	private Logger log = LoggerFactory.getLogger(getClass());
	private ProjectResult result;
	private Project project;

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		super.init(site, editorInput);
		try {
			ProjectResultInput input = (ProjectResultInput) editorInput;
			result = Cache.getAppCache().get(input.getResultKey(),
					ProjectResult.class);
			ProjectDao dao = new ProjectDao(Database.get());
			project = dao.getForId(input.getProjectId());
		} catch (Exception e) {
			log.error("failed to load project result", e);
			throw new PartInitException("failed to load project result", e);
		}
	}

	ProjectResult getResult() {
		return result;
	}

	Project getProject() {
		return project;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProjectResultPage(this));
		} catch (Exception e) {
			log.error("failed to add pages", e);
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

}
