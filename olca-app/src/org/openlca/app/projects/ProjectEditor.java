package org.openlca.app.projects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.util.Calendar;

public class ProjectEditor extends ModelEditor<Project> implements IEditor {

	public static String ID = "editors.project";
	private Logger log = LoggerFactory.getLogger(getClass());

	public ProjectEditor() {
		super(Project.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProjectSetupPage(this));
			addPage(new ProjectInfoPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		getModel().setLastModificationDate(Calendar.getInstance().getTime());
		super.doSave(monitor);
	}

}
