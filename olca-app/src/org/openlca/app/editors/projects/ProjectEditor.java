package org.openlca.app.editors.projects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.Config;
import org.openlca.app.db.Database;
import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.reports.ReportEditorPage;
import org.openlca.app.editors.reports.Reports;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.core.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.util.Calendar;

public class ProjectEditor extends ModelEditor<Project> implements IEditor {

	public static String ID = "editors.project";
	private Logger log = LoggerFactory.getLogger(getClass());

	private Report report;

	public ProjectEditor() {
		super(Project.class);
	}

	public Report getReport() {
		return report;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		report = Reports.createOrOpen(getModel(), Database.get());
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProjectSetupPage(this));
			if (Config.isBrowserEnabled()) {
				addPage(new ReportEditorPage(this, report));
			} else {
				addPage(new ProjectInfoPage(this));
			}
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		getModel().setLastModificationDate(Calendar.getInstance().getTime());
		Reports.save(getModel(), report, Database.get());
		super.doSave(monitor);
	}

}
