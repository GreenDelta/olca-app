package org.openlca.app.editors.projects;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
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
		var reportFile = reportFile();
		if (reportFile.exists()) {
			report = Report.fromFile(reportFile, Database.get()).orElse(null);
			if (report == null) {
				ErrorReporter.on("Failed to read report file: " + reportFile);
			}
		}
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProjectSetupPage(this));
			if (report != null) {
				addPage(new ReportEditorPage(this));
			}
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("Failed to add project pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (report != null) {
			try {
				var file = reportFile();
				var parent = file.getParentFile();
				if (!parent.exists()) {
					Files.createDirectories(parent.toPath());
				}
				Files.writeString(file.toPath(), report.toJson());
			} catch (IOException e) {
				ErrorReporter.on("Failed to write report file", e);
			}
		}
		super.doSave(monitor);
	}

	private File reportFile() {
		var dir = DatabaseDir.getDir(getModel());
		return new File(dir, "report.json");
	}
}
